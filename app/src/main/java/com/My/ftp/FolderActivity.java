package com.My.ftp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

public class FolderActivity extends AppCompatActivity {
    private String localHome;
    private String folderTitle;
    private int currentSelectedCell;
    private String selectedFiename;

    private static final int NEW_FILE = 1;
    private static final int NEW_DIR = 2;
    private static final int UPLOAD = 3;

    private static final String CONNECTIVITY_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String UPLOAD_BREAKPOINT_INFO = "breakpointinfo";

    private static final String LOGTAG = "FTPClient";

    private com.baoyz.swipemenulistview.SwipeMenuListView listview;
    private List<HashMap<String,Object>> simpleAdptList;;
    private SimpleAdapter simpleAdapter;

    private Toolbar toolbar;
    private com.wang.avi.AVLoadingIndicatorView loadingView;
    private com.wang.avi.AVLoadingIndicatorView downLoadingView;
    private com.wang.avi.AVLoadingIndicatorView uploadView;
    private TextView loadText;
    private ProgressBar progressBar;

    private AlertDialog.Builder digGeneral;
    private File file1;
    private String localPath;
    private String serverPath;
    private UploadThread thread;	//上传线程
    private Thread continueThread;	//断点上传线程
    private FTPFile remoteFile;
    private File packageFile;	//需上传的文件

    private long uploadSize = 0L;	//已上传的文件的大小
    private long needUploadSize = 0L;

    private long downloadSize = 0L;
    private long needDownloadSize = 0L;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private BroadcastReceiver connctionChangeReceiver;
    private IntentFilter filter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);
        initViews();
        setupData();
//        setListener();

        preferences = getSharedPreferences(UPLOAD_BREAKPOINT_INFO, MODE_PRIVATE);
        editor = preferences.edit();

        filter = new IntentFilter(CONNECTIVITY_CHANGE_ACTION);
        connctionChangeReceiver = new ConnectionChangeReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InitialActivity.client.changeDirectory(folderTitle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setupData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InitialActivity.client.changeDirectory(folderTitle);
                    FTPFile[] files = InitialActivity.client.list();
                    List<FTPFile> ftpFolderFiles = new ArrayList<>();
                    List<FTPFile> ftpTxtFiles = new ArrayList<>();
                    List<FTPFile> ftpPyFiles = new ArrayList<>();
                    List<FTPFile> ftpPdfFiles = new ArrayList<>();
                    List<FTPFile> ftpSwiftFiles = new ArrayList<>();
                    List<FTPFile> ftpHtmlFiles = new ArrayList<>();
                    List<FTPFile> ftpJavaFiles = new ArrayList<>();
                    List<FTPFile> ftpRarFiles = new ArrayList<>();
                    List<FTPFile> ftpZipFiles = new ArrayList<>();
                    List<FTPFile> ftpOtherFiles = new ArrayList<>();
                    for (FTPFile ftpfile: files) {
                        if (ftpfile.getType() == 1) {
                            ftpFolderFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".img")) {
                            ftpHtmlFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".java")) {
                            ftpJavaFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".pdf")) {
                            ftpPdfFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".py")) {
                            ftpPyFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".jpg")) {
                            ftpSwiftFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".txt")) {
                            ftpTxtFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".zip")) {
                            ftpZipFiles.add(ftpfile);
                        } else if (ftpfile.getName().endsWith(".rar")) {
                            ftpRarFiles.add(ftpfile);
                        } else {
                            ftpOtherFiles.add(ftpfile);
                        }
                    }

                    ftpHtmlFiles.addAll(ftpJavaFiles);
                    ftpHtmlFiles.addAll(ftpPdfFiles);
                    ftpHtmlFiles.addAll(ftpPyFiles);
                    ftpHtmlFiles.addAll(ftpSwiftFiles);
                    ftpHtmlFiles.addAll(ftpTxtFiles);
                    ftpHtmlFiles.addAll(ftpOtherFiles);
                    ftpHtmlFiles.addAll(ftpZipFiles);
                    ftpHtmlFiles.addAll(ftpRarFiles);
                    ftpHtmlFiles.addAll(ftpFolderFiles);

                    //归类显示
                    for(FTPFile file: ftpHtmlFiles) {
                        HashMap<String, Object> hashMap
                                = new HashMap<>();
                        hashMap.put("icon",R.drawable.other);
                        if (file.getType() == 1) {
                            hashMap.put("icon",R.drawable.folder);
                        } else if (file.getName().endsWith(".txt")) {
                            hashMap.put("icon",R.drawable.txt);
                        } else if (file.getName().endsWith(".pdf")) {
                            hashMap.put("icon",R.drawable.pdf);
                        } else if (file.getName().endsWith(".jpg")) {
                            hashMap.put("icon",R.drawable.swift);
                        } else if (file.getName().endsWith(".img")) {
                            hashMap.put("icon",R.drawable.html);
                        } else if (file.getName().endsWith(".java")) {
                            hashMap.put("icon",R.drawable.java);
                        } else if (file.getName().endsWith(".py")) {
                            hashMap.put("icon",R.drawable.py);
                        } else if (file.getName().endsWith(".rar")) {
                            hashMap.put("icon",R.drawable.rar);
                        } else if (file.getName().endsWith(".zip")) {
                            hashMap.put("icon",R.drawable.zip);
                        }

                        hashMap.put("name",file.getName());
                        hashMap.put("isdir",file.getType()==1);
                        hashMap.put("filesize",file.getSize());
                        simpleAdptList.add(hashMap);
                    }

                    handleNotifyDataChanged();

                    handleHideLoadView();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initViews() {
//        localHome = "/data" + Environment.getDataDirectory().getAbsolutePath()
//                + File.separator + getPackageName()
//                + File.separator + "ftpdownload";

//        File file1 = new File(getExternalFilesDir("/"), "ftpdownload");

        File file1 = new File (String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));

        localHome = file1.getPath();

        folderTitle = getIntent().getStringExtra("filename");

        loadingView = findViewById(R.id.avi);
        loadingView.smoothToShow();

        downLoadingView = findViewById(R.id.aviDown);

        uploadView = findViewById(R.id.aviUpload);

        loadText = findViewById(R.id.loadText);

        progressBar = findViewById(R.id.progress_bar);

        toolbar = findViewById(R.id.folder_toolbar);
        toolbar.setTitle("Files in Storage");
        setSupportActionBar(toolbar);

        listview = findViewById(R.id.names_ftpfiles_list_view);
        simpleAdptList = new ArrayList<>();

        String[] from = {"icon", "name"};
        int[] to = {R.id.cell_image, R.id.cell_name};
        simpleAdapter = new SimpleAdapter(FolderActivity.this, simpleAdptList, R.layout.cell, from, to);
        listview.setAdapter(simpleAdapter);

        this.registerForContextMenu(listview);
    }

    private void setListener() {
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(FolderActivity.this);
                    alert.setTitle("Operation in progress").setMessage("You have an operation in progress\n，\n" +
                            "Cannot perform the operation.");
                    alert.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).create().show();
                } else {
                    Boolean isDir = (Boolean) simpleAdptList.get(position).get("isdir");
                    String fileName = (String) simpleAdptList.get(position).get("name");

                    if (isDir) {
                        Intent intent = new Intent(FolderActivity.this, FolderActivity.class);
                        intent.putExtra("filename", folderTitle + File.separator + fileName);
                        if (folderTitle.endsWith(File.separator)) {
                            intent.putExtra("filename", folderTitle + fileName);
                        }
                        startActivity(intent);
                    } else if (!isDir) {
                        currentSelectedCell = position;
                        selectedFiename = fileName;
                        prepareDownload(fileName);
                    }
                }
            }
        });

        uploadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    try {
                        InitialActivity.client.abortCurrentDataTransfer(true);
                        reConnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        downLoadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    try {
                        InitialActivity.client.abortCurrentDataTransfer(true);
                        reConnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem deleteItem = new SwipeMenuItem(getApplicationContext());
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                deleteItem.setWidth(200);
                deleteItem.setIcon(R.drawable.delete);
                menu.addMenuItem(deleteItem);
            }
        };
        listview.setMenuCreator(creator);
        listview.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu menu, final int index) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(FolderActivity.this);
                    alert.setTitle("Operation in progress").setMessage("\n" +
                            "You have an operation in progress，\n" +
                            "Cannot perform the operation.");
                    alert.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).create().show();
                } else {
                    switch (index) {
                        case 0:
                            final Boolean isDir3 = (Boolean) simpleAdptList.get(position).get("isdir");
                            final String fileName3 = (String) simpleAdptList.get(position).get("name");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (isDir3) {
                                            if (folderTitle.contentEquals("/")) {
                                                InitialActivity.client.deleteDirectory(folderTitle+fileName3);
                                            } else {
                                                InitialActivity.client.deleteDirectory(folderTitle + '/' +fileName3);
                                            }
                                        } else if (!isDir3) {
                                            if (folderTitle.contentEquals("/")) {
                                                InitialActivity.client.deleteFile(folderTitle+fileName3);
                                            } else {
                                                InitialActivity.client.deleteFile(folderTitle + '/' +fileName3);
                                            }
                                        }
                                        simpleAdptList.remove(position);

                                        handleNotifyDataChanged();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        default:
                            break;
                    }
                }

                // true : close the menu; false : not close the menu
                return true;
            }
        });
        listview.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);
    }

    private void prepareDownload(final String originFilename) {
        try {
            //确定ftp服务器上该文件路径
            serverPath = folderTitle + File.separator + originFilename;//服务器上的文件
            if (folderTitle.endsWith(File.separator)) {
                serverPath = folderTitle + originFilename;
            }

            //获取本地下载文件夹
            final File file = new File(localHome);
            if (!file.exists()) {
                file.mkdirs();
            }

            final String[] subnames = selectedFiename.split("\\.");

            file1 = new File(file, selectedFiename);

            //检测是否存在，如存在，询问是否覆盖
            if (file1.exists()) {
                digGeneral = new AlertDialog.Builder(FolderActivity.this);
                digGeneral.setTitle("File is duplicated with local file");
                digGeneral.setMessage("This file already exists in the local file" +
                        "Need to overwrite or create a copy？");

                digGeneral.setPositiveButton("Create a copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        int number = 0;

                        if (subnames.length > 1) {
                            while(file1.exists()) {
                                selectedFiename = subnames[subnames.length-2] + (++number) + "." + subnames[subnames.length-1];
                                file1 = new File(file, selectedFiename);
                            }
                        }

                        new downloadThread().start();
                    }
                });
                digGeneral.setNeutralButton("\n" +
                        "Replace", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        new downloadThread().start();
                    }
                });
                digGeneral.setCancelable(true);
                digGeneral.create().show();
            } else {
                new downloadThread().start();
            }
        } catch (Exception e) {
            handleHhowToast("download failed");
            handleDownloadHide();

            reConnect();
        }
    }

    public class downloadThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                //DialogInterface.OnClickListener是下载监听器，在监听器中实现下载进度条
                InitialActivity.client.download(serverPath, file1, new MyDownloadTransferListener());
            } catch (Exception e) {
                downloadSize = 0L;
                e.printStackTrace();

                if (InitialActivity.client == null) {
                    handleHhowToast("download failed");
                    handleDownloadHide();
                }

                reConnect();
            }
        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//        menu.add(0, UPLOAD,  0,  "upload files");
//        menu.add(0, NEW_FILE, 1, "create a new file");
//        menu.add(0, NEW_DIR,  2, "new folder");
//
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_upload, menu);
        return true;
    }

    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            selectedFiename = data.getStringExtra("filename");
            asynUpload();
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (progressBar.getVisibility() == View.VISIBLE) {
            AlertDialog.Builder alert = new AlertDialog.Builder(FolderActivity.this);
            alert.setTitle("Operation in progress").setMessage("You have an operation in progress，Cannot perform the operation.");
            alert.setPositiveButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            }).create().show();
        } else {
            switch (item.getItemId()) {
                case R.id.upload_image:
                    Intent intent = new Intent(FolderActivity.this, LocalHomeActivity.class);
                    intent.putExtra("title", "Please select the file to upload");
                    intent.putExtra("type", "image");
                    startActivityForResult(intent, 0);
                    break;

                case R.id.upload_video:
                    Intent intent2 = new Intent(FolderActivity.this, LocalHomeActivity.class);
                    intent2.putExtra("title", "Please select the file to upload");
                    intent2.putExtra("type", "video");
                    startActivityForResult(intent2, 0);
                    break;

                case R.id.upload_audio:
                    Intent intent3 = new Intent(FolderActivity.this, LocalHomeActivity.class);
                    intent3.putExtra("title", "Please select the file to upload");
                    intent3.putExtra("type", "audio");
                    startActivityForResult(intent3, 0);
                    break;

                case R.id.upload_docs:
                    Intent intent4 = new Intent(FolderActivity.this, LocalHomeActivity.class);
                    intent4.putExtra("title", "Please select the file to upload");
                    intent4.putExtra("type", "docs");
                    startActivityForResult(intent4, 0);
                    break;

//                case NEW_FILE:
//                    final View cusView = LayoutInflater.from(FolderActivity.this).inflate(R.layout.new_file, null);
//                    AlertDialog.Builder cusDia = new AlertDialog.Builder(FolderActivity.this);
//                    cusDia.setTitle("Create new file");
//                    cusDia.setView(cusView);
//
//                    cusDia.setPositiveButton("create", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            final EditText fileName = cusView.findViewById(R.id.fileName);
//                            final EditText fileNameSuffix = cusView.findViewById(R.id.fileNameSuffix);
//
//                            final String subPath = fileName.getText().toString().trim() + "." + fileNameSuffix.getText().toString().trim();
//                            Log.d(LOGTAG, subPath);
//
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        final File file = new File("/data" + Environment.getDataDirectory().getAbsolutePath()
//                                                + File.separator + getPackageName()
//                                                + File.separator +  "ftpupload" + File.separator
//                                                + subPath);
//                                        if (!file.exists()) {
//                                            file.createNewFile();
//                                        }
//
//                                        packageFile = file;
//
//                                        InitialActivity.client.upload(file, new MyUploadTransferListener());
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                        handleHhowToast("Failed to create new file");
//                                    }
//                                }
//                            }).start();
//                        }
//                    });
//
//                    cusDia.create().show();
//                    break;
//                case NEW_DIR:
//                    final View cusView2 = LayoutInflater.from(FolderActivity.this).inflate(R.layout.new_folder, null);
//                    AlertDialog.Builder cusDia2 = new AlertDialog.Builder(FolderActivity.this);
//                    cusDia2.setTitle("Create new file");
//                    cusDia2.setView(cusView2);
//
//                    cusDia2.setPositiveButton("create", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            final EditText folderName = cusView2.findViewById(R.id.folderName);
//                            final String subPath = folderName.getText().toString().trim();
//
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        InitialActivity.client.createDirectory(subPath);
//                                        handleHhowToast("Create new folder successfully");
//
//                                        addListView(subPath, 0, true);
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                        handleHhowToast("Failed to create new folder");
//                                    }
//                                }
//                            }).start();
//                        }
//                    });
//
//                    cusDia2.create().show();
//                    break;
                default:
                    break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        currentSelectedCell = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;//获取listview的item对象

        menu.add(0, 0, 0, "download");
        menu.add(0, 1, 1, "Delete Files");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case 0:
                Boolean isDir = (Boolean) simpleAdptList.get(currentSelectedCell).get("isdir");
                String fileName = (String) simpleAdptList.get(currentSelectedCell).get("name");
                if (isDir) {
                    Toast.makeText(FolderActivity.this, "Folder download is not currently supported", Toast.LENGTH_SHORT).show();
                } else if (!isDir) {
                    selectedFiename = fileName;
                    prepareDownload(fileName);
                }
                break;
            case 1:
                final Boolean isDir3 = (Boolean) simpleAdptList.get(currentSelectedCell).get("isdir");
                final String fileName3 = (String) simpleAdptList.get(currentSelectedCell).get("name");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isDir3) {
                                if (folderTitle.contentEquals("/")) {
                                    InitialActivity.client.deleteDirectory(folderTitle+fileName3);
                                } else {
                                    InitialActivity.client.deleteDirectory(folderTitle + '/' +fileName3);
                                }
                            } else if (!isDir3) {
                                if (folderTitle.contentEquals("/")) {
                                    InitialActivity.client.deleteFile(folderTitle+fileName3);
                                } else {
                                    InitialActivity.client.deleteFile(folderTitle + '/' +fileName3);
                                }
                            }
                            simpleAdptList.remove(currentSelectedCell);

                            handleNotifyDataChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            default:
                break;
            }

        return super.onContextItemSelected(item);
    }


    private boolean needContinue = false;
    public void asynUpload(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    packageFile = new File(localHome + File.separator  + selectedFiename);
                    needUploadSize = packageFile.length();

                    needContinue = false;
                    FTPFile[] ftpfiles = InitialActivity.client.list();
                    for (FTPFile ftpFile : ftpfiles) {
                        if (ftpFile.getName().equals(selectedFiename)) {
                            needContinue = true;
                            uploadSize = ftpFile.getSize();

                            //续传的时候。本地记录的大小和服务器不一致。应该是去掉了头文件之类的缘故。所以续传的时候需要获取服务器该
                            // 文件已传的大小作为续传点而不是本地记录的已传大小。所以此处文件大小不能直接从sharedpreferences里面读出来的。

                            if (needUploadSize <= uploadSize) {
                                handleHhowToast("File already exists");
                            } else {
                                Message msg = new Message();
                                msg.what = 1111111;
                                mHandler.sendMessage(msg);

                                new ContinueUploadThread().start();
                            }

                            return;
                        } else if (ftpfiles[ftpfiles.length-1] == ftpFile) {
                            uploadSize = 0L;
                            new UploadThread().start();
                        }

                    }




                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }




    //MARK: - ftp文件上传线程
    class UploadThread extends Thread {
        @Override
        public void run() {
            try {
                InitialActivity.client.upload(packageFile, new MyUploadTransferListener());
            } catch (FileNotFoundException e) {
                Log.e(LOGTAG,"UploadThread FileNotFoundException");
                e.printStackTrace();
            }  catch (Exception e){
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    //MARK: - 断点上传线程
    public class ContinueUploadThread extends Thread {
        @Override
        public void run() {
            try {
                if (InitialActivity.client.isResumeSupported()) {
                    InitialActivity.client.upload(packageFile, uploadSize, new MyUploadTransferListener());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    //MARK: - ftp文件下载监听器
    public class MyDownloadTransferListener implements FTPDataTransferListener {
        @Override
        public void started() {
            handleDownloadShow(selectedFiename);

            downloadSize = 0L;
            needDownloadSize = (long) simpleAdptList.get(currentSelectedCell).get("filesize");
        }

        @Override
        public void transferred(int i) {
            //计算出下载百分比，在进度条显示
            downloadSize += i;
            int percent = (int)(downloadSize*100/(needDownloadSize*1.0));
            handleNotifyDownChanged(percent);
        }

        @Override
        public void completed() {
            downloadSize = 0L;

            handleHhowToast("download successful");
            handleDownloadHide();

            Intent intent = new Intent(FolderActivity.this, TxtActivity.class);
            if (selectedFiename.endsWith(".pdf")) {
                intent = new Intent(FolderActivity.this, PDFActivity.class);
            } else if (selectedFiename.endsWith(".rar") || selectedFiename.endsWith(".zip") || needDownloadSize>10000000) {
                Message msg = new Message();
                msg.what = 2222222;
                mHandler.sendMessage(msg);

                return;
            }
            intent.putExtra("filepath", "/data" + Environment.getDataDirectory().getAbsolutePath()
                    + File.separator + getPackageName()
                    + File.separator + "ftpdownload" + File.separator + selectedFiename);
            intent.putExtra("filename", selectedFiename);
            startActivity(intent);
        }

        @Override
        public void aborted() {
            handleHhowToast("Download stopped");
            handleDownloadHide();

            reConnect();
        }

        @Override
        public void failed() {
            handleHhowToast("download failed");
            handleDownloadHide();

            reConnect();
        }
    }

    //MARK: - ftp文件上传监听器
    public class MyUploadTransferListener implements FTPDataTransferListener {
        public void started() {
            registerReceiver(connctionChangeReceiver, filter);

            handleUploadShow(selectedFiename);
        }
        public void transferred(int arg0) {
            uploadSize += arg0;
            int percent = (int)(uploadSize*100/(needUploadSize*1.0));

            handleNotifyUpdateChanged(percent);
        }
        public void completed() {
            unregisterReceiver(connctionChangeReceiver);

            uploadSize = 0L;

            handleUploadHide();
            handleHhowToast("Upload completed");

            if (!needContinue) {
                addListView(packageFile.getName(), packageFile.length(), packageFile.isDirectory());
            }
        }
        public void aborted() {
            unregisterReceiver(connctionChangeReceiver);

            handleUploadHide();
            handleHhowToast("Upload stop");

            reConnect();
        }
        public void failed() {
            unregisterReceiver(connctionChangeReceiver);

            handleUploadHide();
            handleHhowToast("Upload failed");

            reConnect();
        }
    }

    //MARK: - 网络信息变化接收广播
    class ConnectionChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

            if (!(activeNetInfo != null && activeNetInfo.isConnected())) {
                try {
                    InitialActivity.client.abortCurrentDataTransfer(true);
                    reConnect();

                    Intent intent2 = new Intent();
                    intent2.setAction("android.intent.action.MAIN");
                    intent2.addCategory("android.intent.category.LAUNCHER");
                    startActivity(intent2);

                    handleHhowToast("Network error");
                    handleUploadHide();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FTPIllegalReplyException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    //MARK: - 交互消息处理
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Helper.SHOW_TOAST:
                    Toast.makeText(FolderActivity.this, (String)msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case Helper.SET_TITLE:
                    toolbar.setSubtitle((String)msg.obj);
                    break;

                case Helper.NOTIFY_DATA_CHANGED:
                    simpleAdapter.notifyDataSetChanged();
                    break;

                case Helper.SHOW_LOAD_VIEW:
                    loadingView.smoothToShow();
                    break;
                case Helper.HIDE_LOAD_VIEW:
                    loadingView.smoothToHide();
                    loadText.setText("Current user: " + InitialActivity.currentUser);
                    progressBar.setVisibility(View.INVISIBLE);
                    break;

                case Helper.DOWNLOAD_SHOW:
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                    downLoadingView.smoothToShow();
                    loadText.setText("Downloading: "+(String)msg.obj+ " ...");
                    loadText.setVisibility(View.VISIBLE);
                    break;
                case Helper.DOWNLOAD_CHANGE:
                    progressBar.setProgress((int)msg.obj);
                    break;
                case Helper.DOWNLOAD_HIDE:
                    loadText.setText("Current user: " + InitialActivity.currentUser);
                    downLoadingView.smoothToHide();
                    progressBar.setVisibility(View.INVISIBLE);
                    break;

                case Helper.UPLOAD_SHOW:
                    uploadView.smoothToShow();
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                    uploadView.smoothToShow();
                    loadText.setVisibility(View.VISIBLE);
                    loadText.setText("Uploading: " + (String)msg.obj+ " ...");
                    break;
                case Helper.UPLOAD_CHANGE:
                    if (msg.obj != null) {
                        progressBar.setProgress((int)msg.obj);
                    }
                    break;
                case Helper.UPLOAD_HIDE:
                    uploadView.smoothToHide();
                    loadText.setText("Current user: " + InitialActivity.currentUser);
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case 1111111:
                    AlertDialog.Builder alert = new AlertDialog.Builder(FolderActivity.this);
                    alert.setTitle("Breakpoint upload").setMessage("\n" +
                            "Part of the file has been uploaded，Will upload from the breakpoint");
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).setCancelable(true).create().show();
                    break;
                case 2222222:
                    AlertDialog.Builder alert2 = new AlertDialog.Builder(FolderActivity.this);
                    alert2.setTitle("File download complete").setMessage("The file format does not support viewing or the file is too large，Please use another App to open the file");
                    alert2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setCancelable(true).create().show();
                    break;
            }
        }
    };

    private void handleHhowToast(String message) {
        Message msg = new Message();
        msg.what = Helper.SHOW_TOAST;
        msg.obj = message;
        mHandler.sendMessage(msg);
    }

    private void handleHideLoadView() {
        Message msg = new Message();
        msg.what = Helper.HIDE_LOAD_VIEW;
        mHandler.sendMessage(msg);
    }

    private void handleNotifyDataChanged() {
        Message msg = new Message();
        msg.what = Helper.NOTIFY_DATA_CHANGED;
        mHandler.sendMessage(msg);
    }

    private void handleUploadShow(String filename) {
        Message msg = new Message();
        msg.what = Helper.UPLOAD_SHOW;
        msg.obj = filename;
        mHandler.sendMessage(msg);
    }
    private void handleNotifyUpdateChanged(int percent) {
        Message msg = new Message();
        msg.what = Helper.UPLOAD_CHANGE;
        msg.obj = percent;
        mHandler.sendMessage(msg);
    }
    private void handleUploadHide() {
        Message msg = new Message();
        msg.what = Helper.UPLOAD_HIDE;
        mHandler.sendMessage(msg);
    }

    private void handleDownloadShow(String fileName) {
        Message msg = new Message();
        msg.what = Helper.DOWNLOAD_SHOW;
        msg.obj = fileName;
        mHandler.sendMessage(msg);
    }
    private void handleNotifyDownChanged(int percent) {
        Message msg = new Message();
        msg.what = Helper.DOWNLOAD_CHANGE;
        msg.obj = percent;
        mHandler.sendMessage(msg);
    }
    private void handleDownloadHide() {
        Message msg = new Message();
        msg.what = Helper.DOWNLOAD_HIDE;
        mHandler.sendMessage(msg);
    }

    private void addListView(String fileName, long fileSize, Boolean isDir) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("isdir", false);
        hashMap.put("icon", R.drawable.other);
        if (isDir) {
            hashMap.put("icon", R.drawable.folder);
            hashMap.put("isdir", true);
        } else if (fileName.endsWith(".txt")) {
            hashMap.put("icon",R.drawable.txt);
        } else if (fileName.endsWith(".pdf")) {
            hashMap.put("icon",R.drawable.pdf);
        } else if (fileName.endsWith(".swift")) {
            hashMap.put("icon",R.drawable.swift);
        } else if (fileName.endsWith(".html")) {
            hashMap.put("icon",R.drawable.html);
        } else if (fileName.endsWith(".java")) {
            hashMap.put("icon",R.drawable.java);
        } else if (fileName.endsWith(".py")) {
            hashMap.put("icon",R.drawable.py);
        } else if (fileName.endsWith(".rar")) {
            hashMap.put("icon",R.drawable.rar);
        } else if (fileName.endsWith(".zip")) {
            hashMap.put("icon",R.drawable.zip);
        }

        hashMap.put("name", fileName);
        hashMap.put("filesize", fileSize);
        simpleAdptList.add(hashMap);

        handleNotifyDataChanged();
    }


    //MARK: - 返回按键监听
    @Override
    public void onBackPressed() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            AlertDialog.Builder alert = new AlertDialog.Builder(FolderActivity.this);
            alert.setTitle("Operation in progress").setMessage("You have an operation in progress，Are you sure to quit？");
            alert.setNeutralButton("drop out", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        InitialActivity.client.abortCurrentDataTransfer(true);
                        reConnect();
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            alert.setPositiveButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).create().show();
        } else {
            super.onBackPressed();
        }
    }

    void reConnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InitialActivity.login();
                    InitialActivity.client.changeDirectory(folderTitle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
