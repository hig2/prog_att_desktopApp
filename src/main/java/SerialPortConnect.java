import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SerialPortConnect {
    private final static int BAUD_RATE_DEFAULT = 57600;
    private static SerialPort serialPort;
    private static  ArrayList<String> serialPortsList = new ArrayList<>(0);
    private static String openPortName = "";
    private int openPortBaudRate;
    private static SerialPortConnect connect;
    static SerialPort[] serialPorts;
    private static boolean exchangeFlag = false;

    //часть парсера
    private static short[] inArray = new short[4];
    private static short[] outArray = new short[4];
    private static byte[] globalBuffer = new byte[inArray.length * 5];
    private static int indexGlobalBuffer = 0;
    private static boolean startReadFlag = false;
    private static int realByte = 0;


    private static final char startSymbol = '$';
    private static final char finishSymbol = ';';
    private static final char separatorSymbol = ',';



    private SerialPortConnect(String openPortName) {
        serialPort = SerialPort.getCommPort(openPortName);
        serialPort.openPort();
        serialPort.setBaudRate(BAUD_RATE_DEFAULT);
        System.out.println("Порт открыт:  " + openPortName);
        this.openPortName = openPortName;

    }

    public static void setOutArray(short value, int index){
        outArray[index] = value;
    }



    public static ArrayList<String> getSerialPortsList() {
        return serialPortsList;
    }

    public static boolean connecting(String openPortName) throws Exception {
        if(connect == null){
            if(serialPortsList.stream().anyMatch(e -> e.equals(openPortName))){
                connect = new SerialPortConnect(openPortName);
                exchangeTask();
                disconnectWatcher();
                System.out.println("Подключен");
                return true;
            }else{
                throw new Exception("Порт не обнаружен");
            }
        }
        return false;
    }

    public static SerialPortConnect getConnect(){
        return connect;
    }

    public static short[] getInArray(){
        return inArray;
    }


    public static boolean isConnected(){
        return SerialPortConnect.connect != null;
    }


    // таск на апдейт списка ком портов
    synchronized public static void serialPortsListTask(){
        Thread thread = new Thread(() -> {
            while (true) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                    ArrayList<String> secondSerialPortList = new ArrayList<>();


                    for(SerialPort e: SerialPort.getCommPorts()){
                        secondSerialPortList.add(e.getSystemPortName());
                    }


                    //если нет не одного порта производим дисконнект
                    if(secondSerialPortList.size() == 0){
                        close();
                    }
                    serialPortsList = secondSerialPortList;
            }
        });
        thread.start();
    }


    public static void close(){
        if(serialPort != null && serialPort.isOpen()){
            serialPort.closePort();
        }
        openPortName = "";
        exchangeFlag = false;
        connect = null;
    }

    private static void readTask(long delay) throws IOException, InterruptedException {
        AtomicLong delayTimer = new AtomicLong(0);
        final AtomicLong exchangeTimer = new AtomicLong(0);
        Thread thread = new Thread(() -> {
            while (connect != null) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if ((System.currentTimeMillis() - delayTimer.get()) > delay) {
                    delayTimer.set(System.currentTimeMillis());

                    //чтение и проверка на обмен
                    if(readSerial()){
                        exchangeTimer.set(System.currentTimeMillis());
                        exchangeFlag = true;
                    }else if((System.currentTimeMillis() - exchangeTimer.get()) > 1000){
                        exchangeFlag = false;
                    }
                }
            }
        });
        thread.start();
    }


    private static void disconnectWatcher(){
        long t = System.currentTimeMillis();
        Thread thread = new Thread(()->{
            while (connect != null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if((System.currentTimeMillis() - t) > 5000){
                    if(!isExchangeFlag()){
                        close();
                    }
                }

            }
        });
        thread.start();
    }



    public static boolean isExchangeFlag(){
        return exchangeFlag;
    }


    private static boolean readSerial(){
        while (serialPort.bytesAvailable() > 0) {

        }
        return false;
    }

    private static void writePaket(){
        short crc = 0;
        String result = String.valueOf(startSymbol);

        for (int i = 0; i < (outArray.length - 1); i++) {
            crc += outArray[i];
        }

        for (int i = 0; i < outArray.length; i++) {
            result = i == (outArray.length - 1) ? result + crc + finishSymbol + '\n' : result + outArray[i] + separatorSymbol;
        }

        byte[] bytesMassage = result.getBytes();
        serialPort.writeBytes(bytesMassage, bytesMassage.length);
    }

    private static  void exchangeTask(){
        AtomicLong t = new AtomicLong();

        Thread thread = new Thread(()->{
            while (connect != null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(System.currentTimeMillis() - t.get() > 200){
                    exchangeFlag = false;
                }

                while (serialPort.bytesAvailable() > 0) {
                    byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                    if(parseBuffer(readBuffer, numRead)){
                        //отправляем пакет
                        writePaket();
                        t.set(System.currentTimeMillis());
                        exchangeFlag = true;
                    }

                }

            }
        });
        thread.start();
    }

    private static boolean parseBuffer(byte[] readBuffer, int numRead){

            for (int i = 0; i < numRead; i++) {
                if (readBuffer[i] == startSymbol) {
                    indexGlobalBuffer = 0;
                    startReadFlag = true;
                    realByte = 0;
                 continue;
            } else if (readBuffer[i] == finishSymbol) {
                //пакет найден
                parsePacket(globalBuffer, realByte);
                realByte = 0;
                startReadFlag = false;
                indexGlobalBuffer = 0;
                return true;

            }

            if (startReadFlag) {
                if(indexGlobalBuffer == globalBuffer.length){
                    realByte = 0;
                    startReadFlag = false;
                    indexGlobalBuffer = 0;
                    return false;
                }
                globalBuffer[indexGlobalBuffer++] = readBuffer[i];
                realByte++;
            }
        }
        return false;
    }

    private static void parsePacket(byte[] newInArray, int realByte) {
        short[] bufferArray = new short[inArray.length];

        //realByte + 1 обрабатываем тем самым последнюю итерацию acc
        for (int i = 0, acc = 0, factor = 0, indexOfBufferArray = 0; i < realByte + 1; i++) {
            if (i == realByte) {
                bufferArray[indexOfBufferArray] = (short) acc;
                break;
            }

            if (newInArray[i] == separatorSymbol) {
                bufferArray[indexOfBufferArray] = (short) acc;
                indexOfBufferArray++;
                if (indexOfBufferArray == (bufferArray.length)) {
                    System.out.println("пришедший пакет больше ожидаемого");
                    // пришедший пакет больше ожидаемого
                    return;
                }
                acc = 0;
                factor = 0;
            } else if ((newInArray[i] - 48) >= 0 && (newInArray[i] - 48) <= 9) {
                acc = ((acc * factor) + (newInArray[i] - 48));
                factor = 10;
            } else {
                System.out.println("была ошибка валидности пакета");
                System.out.println((char) newInArray[i]);
                // была ошибка валидности пакета
                return;
            }

        }

        // начало проверки контрольной суммы
        short crc = 0;

        for (int n = 0; n < bufferArray.length - 1; n++) {
            crc += bufferArray[n];
        }

        if (bufferArray[bufferArray.length - 1] == crc) {
            //все ок
            inArray = bufferArray;
            return ;
        } else {
            // была ошибка crc
            System.out.println("была ошибка crc");
            return;
        }
    }

}