package com.tezcansarizeybek.esc_bluetooth_serial;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gprinter.io.*;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

enum PrinterCommand {
    ESC,
    TSC,
    CPCL
}

public class DeviceConnFactoryManager {

    public PortManager mPort;

    private static final String TAG = DeviceConnFactoryManager.class.getSimpleName();

    public CONN_METHOD connMethod;

    private final String ip;

    private final int port;

    private final String macAddress;

    private final UsbDevice mUsbDevice;

    private final Context mContext;

    private final String serialPortPath;

    private final int baudrate;

    private final int id;

    private final static DeviceConnFactoryManager[] deviceConnFactoryManagers = new DeviceConnFactoryManager[4];

    private boolean isOpenPort;
    /**
     * ESC查询打印机实时状态指令 // ESC query printer real-time status instruction
     */
    private final byte[] esc = {0x10, 0x04, 0x02};

    /**
     * ESC查询打印机实时状态 缺纸状态
     */
    private static final int ESC_STATE_PAPER_ERR = 0x20;

    /**
     * ESC指令查询打印机实时状态 打印机开盖状态
     */
    private static final int ESC_STATE_COVER_OPEN = 0x04;

    /**
     * ESC指令查询打印机实时状态 打印机报错状态
     */
    private static final int ESC_STATE_ERR_OCCURS = 0x40;

    /**
     * TSC查询打印机状态指令
     */
    private final byte[] tsc = {0x1b, '!', '?'};

    /**
     * TSC指令查询打印机实时状态 打印机缺纸状态
     */
    private static final int TSC_STATE_PAPER_ERR = 0x04;

    /**
     * TSC指令查询打印机实时状态 打印机开盖状态
     */
    private static final int TSC_STATE_COVER_OPEN = 0x01;

    /**
     * TSC指令查询打印机实时状态 打印机出错状态
     */
    private static final int TSC_STATE_ERR_OCCURS = 0x80;

    private final byte[] cpcl={0x1b,0x68};

    /**
     * CPCL指令查询打印机实时状态 打印机缺纸状态
     */
    private static final int CPCL_STATE_PAPER_ERR = 0x01;
    /**
     * CPCL指令查询打印机实时状态 打印机开盖状态
     */
    private static final int CPCL_STATE_COVER_OPEN = 0x02;

    private byte[] sendCommand;
    /**
     * 判断打印机所使用指令是否是ESC指令
     */
    private PrinterCommand currentPrinterCommand;
    public static final byte FLAG = 0x10;
    private static final int READ_DATA = 10000;
    private static final String READ_DATA_CNT = "read_data_cnt";
    private static final String READ_BUFFER_ARRAY = "read_buffer_array";
    public static final String ACTION_CONN_STATE = "action_connect_state";
    public static final String ACTION_QUERY_PRINTER_STATE = "action_query_printer_state";
    public static final String STATE = "state";
    public static final String DEVICE_ID = "id";
    public static final int CONN_STATE_DISCONNECT = 0x90;
    public static final int CONN_STATE_CONNECTED = CONN_STATE_DISCONNECT << 3;
    public PrinterReader reader;
    private int queryPrinterCommandFlag;
    private final int ESC = 1;
    private final int TSC = 3;
    private final int CPCL = 2;
    public enum CONN_METHOD {
        //蓝牙连接
        BLUETOOTH("BLUETOOTH"),
        //USB连接
        USB("USB"),
        //wifi连接
        WIFI("WIFI"),
        //串口连接
        SERIAL_PORT("SERIAL_PORT");

        private final String name;

        CONN_METHOD(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return this.name;
        }
    }

    public static DeviceConnFactoryManager[] getDeviceConnFactoryManagers() {
        return deviceConnFactoryManagers;
    }

    public void openPort() {
        deviceConnFactoryManagers[id].isOpenPort = false;

        switch (deviceConnFactoryManagers[id].connMethod) {
            case BLUETOOTH:
                mPort = new BluetoothPort(macAddress);
                isOpenPort = deviceConnFactoryManagers[id].mPort.openPort();
                break;
            case USB:
                mPort = new UsbPort(mContext, mUsbDevice);
                isOpenPort = mPort.openPort();
                break;
            case WIFI:
                mPort = new EthernetPort(ip, port);
                isOpenPort = mPort.openPort();
                break;
            case SERIAL_PORT:
                mPort = new SerialPort(serialPortPath, baudrate, 0);
                isOpenPort = mPort.openPort();
                break;
            default:
                break;
        }

        if (isOpenPort) {
            queryCommand();
        } else {
            if (this.mPort != null) {
                this.mPort=null;
            }

        }
    }

    /**
     * 查询当前连接打印机所使用打印机指令（ESC（EscCommand.java）、TSC（LabelCommand.java））
     */
    private void queryCommand() {
        //开启读取打印机返回数据线程
        reader = new PrinterReader();
        reader.start(); //读取数据线程
        //查询打印机所使用指令
        queryPrinterCommand(); //小票机连接不上  注释这行，添加下面那三行代码。使用ESC指令

    }

    public CONN_METHOD getConnMethod() {
        return connMethod;
    }

    public boolean getConnState() {
        return isOpenPort;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public UsbDevice usbDevice() {
        return mUsbDevice;
    }

    public void closePort(int id) {
        if (this.mPort != null) {
            if(reader!=null) {
                reader.cancel();
                reader = null;
            }
            boolean b= this.mPort.closePort();
            if(b) {
                this.mPort=null;
                isOpenPort = false;
                currentPrinterCommand = null;
            }
        }

    }

    public String getSerialPortPath() {
        return serialPortPath;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public static void closeAllPort() {
        for (DeviceConnFactoryManager deviceConnFactoryManager : deviceConnFactoryManagers) {
            if (deviceConnFactoryManager != null) {
                Log.e(TAG, "cloaseAllPort() id -> " + deviceConnFactoryManager.id);
                deviceConnFactoryManager.closePort(deviceConnFactoryManager.id);
                deviceConnFactoryManagers[deviceConnFactoryManager.id] = null;
            }
        }
    }

    private DeviceConnFactoryManager(Build build) {
        this.connMethod = build.connMethod;
        this.macAddress = build.macAddress;
        this.port = build.port;
        this.ip = build.ip;
        this.mUsbDevice = build.usbDevice;
        this.mContext = build.context;
        this.serialPortPath = build.serialPortPath;
        this.baudrate = build.baudrate;
        this.id = build.id;
        deviceConnFactoryManagers[id] = this;
    }

    public PrinterCommand getCurrentPrinterCommand() {
        return deviceConnFactoryManagers[id].currentPrinterCommand;
    }

    public static final class Build {
        private String ip;
        private String macAddress;
        private UsbDevice usbDevice;
        private int port;
        private CONN_METHOD connMethod;
        private Context context;
        private String serialPortPath;
        private int baudrate;
        private int id;

        public Build setIp(String ip) {
            this.ip = ip;
            return this;
        }

        public Build setMacAddress(String macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        public Build setUsbDevice(UsbDevice usbDevice) {
            this.usbDevice = usbDevice;
            return this;
        }

        public Build setPort(int port) {
            this.port = port;
            return this;
        }

        public Build setConnMethod(CONN_METHOD connMethod) {
            this.connMethod = connMethod;
            return this;
        }

        public Build setContext(Context context) {
            this.context = context;
            return this;
        }

        public Build setId(int id) {
            this.id = id;
            return this;
        }

        public Build setSerialPort(String serialPortPath) {
            this.serialPortPath = serialPortPath;
            return this;
        }

        public Build setBaudrate(int baudrate) {
            this.baudrate = baudrate;
            return this;
        }

        public DeviceConnFactoryManager build() {
            return new DeviceConnFactoryManager(this);
        }
    }

    public void sendDataImmediately(final Vector<Byte> data) {
        if (this.mPort == null) {
            return;
        }
        try {
            this.mPort.writeDataImmediately(data, 0, data.size());
        } catch (Exception e) {
            // Abort Send
            mHandler.obtainMessage(Constant.abnormal_Disconnection).sendToTarget();
            e.printStackTrace();

        }
    }
    public void sendByteDataImmediately(final byte [] data) {
        if (this.mPort != null) {
            Vector<Byte> datas = new Vector<>();
            for (byte datum : data) {
                datas.add(datum);
            }
            try {
                this.mPort.writeDataImmediately(datas, 0, datas.size());
            } catch (IOException e) {
                // Abort Send
                e.printStackTrace();
                mHandler.obtainMessage(Constant.abnormal_Disconnection).sendToTarget();
            }
        }
    }
    public int readDataImmediately(byte[] buffer){
        int r = 0;
        if (this.mPort == null) {
            return r;
        }

        try {
            r =  this.mPort.readData(buffer);
        } catch (IOException ignored) {

        }

        return  r;
    }

    private void queryPrinterCommand() {
        queryPrinterCommandFlag = ESC;
        ThreadPool.getInstantiation().addSerialTask(() -> {
            final ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder("Timer");
            final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder);
            scheduledExecutorService.scheduleAtFixedRate(threadFactoryBuilder.newThread(() -> {
                if (currentPrinterCommand == null && queryPrinterCommandFlag > TSC) {
                    if (reader != null) {
                        reader.cancel();
                        mPort.closePort();
                        isOpenPort = false;

                        scheduledExecutorService.shutdown();
                    }
                }
                if (currentPrinterCommand != null) {
                    if (!scheduledExecutorService.isShutdown()) {
                        scheduledExecutorService.shutdown();
                    }
                    return;
                }
                switch (queryPrinterCommandFlag) {
                    case ESC:
                        sendCommand = esc;
                        break;
                    case TSC:
                        sendCommand = tsc;
                        break;
                    case CPCL:
                        sendCommand = cpcl;
                        break;
                    default:
                        break;
                }
                Vector<Byte> data = new Vector<>(sendCommand.length);
                for (byte b : sendCommand) {
                    data.add(b);
                }
                sendDataImmediately(data);
                queryPrinterCommandFlag++;
            }), 1500, 1500, TimeUnit.MILLISECONDS);
        });
    }

    class PrinterReader extends Thread {
        private boolean isRun;

        private final byte[] buffer = new byte[100];

        public PrinterReader() {
            isRun = true;
        }

        @Override
        public void run() {
            try {
                while (isRun) {
                    Log.e(TAG,"wait read ");
                    int len = readDataImmediately(buffer);
                    Log.e(TAG," read "+len);
                    if (len > 0) {
                        Message message = Message.obtain();
                        message.what = READ_DATA;
                        Bundle bundle = new Bundle();
                        bundle.putInt(READ_DATA_CNT, len);
                        bundle.putByteArray(READ_BUFFER_ARRAY, buffer);
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                if (deviceConnFactoryManagers[id] != null) {
                    closePort(id);
                    mHandler.obtainMessage(Constant.abnormal_Disconnection).sendToTarget();
                }
            }
        }

        public void cancel() {
            isRun = false;
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.abnormal_Disconnection:
                    Log.d(TAG, "abnormal disconnection");
                    sendStateBroadcast(Constant.abnormal_Disconnection);
                    break;
                case READ_DATA:
                    int cnt = msg.getData().getInt(READ_DATA_CNT);
                    byte[] buffer = msg.getData().getByteArray(READ_BUFFER_ARRAY);
                    if (buffer == null) {
                        return;
                    }
                    int result = judgeResponseType(buffer[0]);
                    String status = "";
                    if (sendCommand == esc) {
                        if (currentPrinterCommand == null) {
                            currentPrinterCommand = PrinterCommand.ESC;
                            sendStateBroadcast(CONN_STATE_CONNECTED);
                        } else {
                            if (result == 0) {
                                Intent intent = new Intent(ACTION_QUERY_PRINTER_STATE);
                                intent.putExtra(DEVICE_ID, id);
                                if(mContext!=null){
                                    mContext.sendBroadcast(intent);
                                }
                            } else if (result == 1) {
                                if ((buffer[0] & ESC_STATE_PAPER_ERR) > 0) {
                                    status += " Printer out of paper";
                                }
                                if ((buffer[0] & ESC_STATE_COVER_OPEN) > 0) {
                                    status += " Printer open cover";
                                }
                                if ((buffer[0] & ESC_STATE_ERR_OCCURS) > 0) {
                                    status += " Printer error";
                                }
                                Log.d(TAG, status);
                            }
                        }
                    }else if (sendCommand == tsc) {
                        if (currentPrinterCommand == null) {
                            currentPrinterCommand = PrinterCommand.TSC;
                            sendStateBroadcast(CONN_STATE_CONNECTED);
                        } else {
                            if (cnt == 1) {
                                if ((buffer[0] & TSC_STATE_PAPER_ERR) > 0) {
                                    status += " Printer out of paper";
                                }
                                if ((buffer[0] & TSC_STATE_COVER_OPEN) > 0) {
                                    status += " Printer open cover";
                                }
                                if ((buffer[0] & TSC_STATE_ERR_OCCURS) > 0) {
                                    status += " Printer error";
                                }
                                Log.d(TAG, status);
                            } else {
                                Intent intent = new Intent(ACTION_QUERY_PRINTER_STATE);
                                intent.putExtra(DEVICE_ID, id);
                                if(mContext!=null){
                                    mContext.sendBroadcast(intent);
                                }
                            }
                        }
                    }else if(sendCommand==cpcl){
                        if (currentPrinterCommand == null) {
                            currentPrinterCommand = PrinterCommand.CPCL;
                            sendStateBroadcast(CONN_STATE_CONNECTED);
                        }else {
                            if (cnt == 1) {

                                if ((buffer[0] ==CPCL_STATE_PAPER_ERR)) {
                                    status += " Printer out of paper";
                                }
                                if ((buffer[0] ==CPCL_STATE_COVER_OPEN)) {
                                    status += " Printer open cover";
                                }
                                Log.d(TAG, status);
                            } else {
                                Intent intent = new Intent(ACTION_QUERY_PRINTER_STATE);
                                intent.putExtra(DEVICE_ID, id);
                                if(mContext!=null){
                                    mContext.sendBroadcast(intent);
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void sendStateBroadcast(int state) {
        Intent intent = new Intent(ACTION_CONN_STATE);
        intent.putExtra(STATE, state);
        intent.putExtra(DEVICE_ID, id);
        if(mContext != null){
            mContext.sendBroadcast(intent);
        }
    }

    private int judgeResponseType(byte r) {
        return (byte) ((r & FLAG) >> 4);
    }

}