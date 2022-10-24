
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;


public class MyController implements Initializable{
    private long timerDelay = 0;
    private boolean firstStartFlag = false;

    private void startShowThread(){
        //preInits
        SerialPortConnect.serialPortsListTask();

        Thread showThread = new Thread(()->{
            while (true){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //tasks
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        showComPortsList();
                        showGeneralWindow();
                        showStatusConnectButton();
                        showStatus();

                    }
                });
            }
        });
        showThread.start();
    }


    @FXML
    private Label connectLabelText;

    @FXML
    private Label statusLabel;

    @FXML
    private Label conLabel;

    @FXML
    private Label attLabel;

    @FXML
    private Label attStatus;

    @FXML
    private Label attnputLabel;

    @FXML
    private Label attParInput;


    @FXML
    private ComboBox<String> comPortsList;


    @FXML
    private Button connectButton;

    @FXML
    private Button applyButton;


    @FXML
    private TextField attInput;




    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startShowThread();
    }



    private void showComPortsList (){
        //System.out.println(SerialPortConnect.getSerialPortsList().size());
        if (SerialPortConnect.getSerialPortsList().size() > 0){
            connectLabelText.setDisable(false);
            comPortsList.setDisable(false);
            connectButton.setDisable(false);
            ObservableList<String> serialPortsList = FXCollections.observableArrayList(SerialPortConnect.getSerialPortsList());
            comPortsList.setItems(serialPortsList);
            //первый из списка выбираем
            if(comPortsList.getValue() == null){
                comPortsList.setValue(serialPortsList.get(0));
            }
        }else{
            connectLabelText.setDisable(true);
            comPortsList.setDisable(true);
            connectButton.setDisable(true);
            comPortsList.valueProperty().set(null);
        }

    }

    public void connectButtonOnAction(ActionEvent actionEvent) {
        if(SerialPortConnect.isConnected()){
            System.out.println("Отключение.");
            SerialPortConnect.close();
        }else{
            System.out.println("Подключение: " + comPortsList.getValue());
            try {
                SerialPortConnect.connecting(comPortsList.getValue());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void showGeneralWindow(){
        boolean flagDisableGeneralWindow = !SerialPortConnect.isConnected();

        statusLabel.setDisable(flagDisableGeneralWindow);
        conLabel.setDisable(flagDisableGeneralWindow);
        attLabel.setDisable(flagDisableGeneralWindow);
        attStatus.setDisable(flagDisableGeneralWindow);
        attStatus.setDisable(flagDisableGeneralWindow);
        attnputLabel.setDisable(flagDisableGeneralWindow);
        attnputLabel.setDisable(flagDisableGeneralWindow);
        attInput.setDisable(flagDisableGeneralWindow);
        attParInput.setDisable(flagDisableGeneralWindow);
        //applyButton.setDisable(flagDisableGeneralWindow);

        if(SerialPortConnect.isConnected() && SerialPortConnect.isExchangeFlag()){
            applyButton.setDisable(false);
        }else{
            applyButton.setDisable(true);
        }

    }

    private void showStatusConnectButton(){
        if(SerialPortConnect.isConnected()){
            connectButton.setText("Отключиться");
        }else{
            connectButton.setText("Подключиться");
        }
    }

    private void showStatus(){
        // так же нужно делать проверку на обмен
        String att;

        if(SerialPortConnect.isConnected() && SerialPortConnect.isExchangeFlag()){
            att = (float)SerialPortConnect.getInArray()[0] / 2  + " dBm";

            // здесь нужна проверка на режим работы
        }else{
            att = "n/a";
        }

        attStatus.setText(att);
    }

    public void applyButtonOnAction(ActionEvent actionEvent) {
        float val = Float.parseFloat(attInput.getText());

        BigDecimal bigDecimal = new BigDecimal(val);

        //val = bigDecimal.setScale(0, BigDecimal.ROUND_HALF_DOWN).floatValue();

        if(val >= 0 && val <= 31.5){
            //округление до 0.5
            val = val % 0.5 != 0 ? val : (float) ((float) Math.floor(val) + 0.5);

            System.out.println("Данные валидны.");
            SerialPortConnect.setOutArray((short) ((short) val * 2), 0);
            return;
        }
        System.out.println("Данные не валидны");
    }


}