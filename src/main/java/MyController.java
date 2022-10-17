import javafx.application.Platform;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;


public class MyController implements Initializable{
    private long timerDelay = 0;
    private boolean firstStartFlag = false;

    private void startShowThread(){
        //preInits

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


                    }
                });
            }
        });
        showThread.start();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startShowThread();
    }

}