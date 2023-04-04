package controller;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Paint;
import object.Checkout;
import object.Customer;
import org.joda.time.format.DateTimeFormat;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class OutputController extends Controller {
    public ListView<Label> logListView;
    public ProgressBar processBar;

    public void initialize(URL location, ResourceBundle resources) {

    }

    public void storeInitEvent() {
        logListView.getItems().clear();
        processBar.setProgress(0);

        int normal = (int) model.checkouts.stream().filter(checkout -> checkout.getType() == Checkout.CheckoutType.NORMAL).count();
        int expressway = (int) model.checkouts.stream().filter(checkout -> checkout.getType() == Checkout.CheckoutType.EXPRESSWAY).count();

        model.outputController.addLog("[ Սուպերմարկետ ] պատրաստ ", Level.CONFIG);
        model.outputController.addLog(" [ Սուպերմարկետ ] Աշխատում է " + normal + " դրամարկղ ", Level.CONFIG);
        if (expressway > 0) {
            model.outputController.addLog("[ Սուպերմարկետ ] Աշխատում է " + expressway + " էքսպրես դրամարկղ ", Level.CONFIG);
        }

        List<String> names = model.checkouts.stream().map(checkout -> "դրամարկղ" + checkout.getCounter().getNo()).collect(Collectors.toList());
        model.statisticsController.initStatistics(names);
    }

    public void customerComeEvent(Customer customer) {
        addLog("[ Հաճախորդ ] [ ՆՈՐ ] Հաճախորդ " + customer.getNo() + " Ապրանք: " + customer.getQuantityOfGoods() + ",Տրամադրություն:" +
                (customer.isCannotWait() ? "Վատ, Հեռանալ " + customer.getWaitSec() + " վ հետո " : "Լավ"), Level.FINE);
    }

    public void customerCheckoutEvent(Checkout checkout, Customer customer) {
        addLog("[ դրամարկղ " + checkout.getCounter().getNo() + " ] Սպասարկված Հաճախորդ " + customer.getNo(), Level.INFO);
    }

    public void customerLeaveEvent(Customer customer, int leaveEvent) {
        if (leaveEvent == LeaveEvent.TOO_LONG_QUEUE) {
            addLog("[ Հաճախորդ ] [ Հեռանալ ] Հաճախորդ " + customer.getNo()
                    + " Հեռացել է զայրույթով, քանի որ հերթերը շատ երկար են: ", Level.WARNING);
        } else {
            addLog("[ Հաճախորդ ] [ Հեռանալ ] Հաճախորդ " + customer.getNo()
                    + " սպասելուց հետո հեռացավ " + customer.getWaitSec() + " վ", Level.WARNING);
        }
    }

    public void addLog(String text, Level level) {
        FontIcon fontIcon = new FontIcon();
        fontIcon.setIconLiteral("fas-plus-circle");
        fontIcon.setIconColor(Paint.valueOf("#00ad57"));

        if (level == Level.WARNING) {
            fontIcon.setIconLiteral("fas-exclamation-circle");
            fontIcon.setIconColor(Paint.valueOf("#e00000"));
        } else if (level == Level.CONFIG) {
            fontIcon.setIconLiteral("fas-info-circle");
            fontIcon.setIconColor(Paint.valueOf("#0070e0"));
        } else if (level == Level.FINE) {
            fontIcon.setIconLiteral("fas-plus-circle");
            fontIcon.setIconColor(Paint.valueOf("#00ad57"));
        } else {
            fontIcon.setIconLiteral("fas-check-circle");
            fontIcon.setIconColor(Paint.valueOf("#00ad57"));
        }
        Label label = new Label("[" + DateTimeFormat.forPattern("HH:mm:ss")
                .print(model.simulatorController.getSimulateTime()) + "] " + text, fontIcon);
        Platform.runLater(() -> logListView.getItems().add(label));
    }

    public static class LeaveEvent {
        public static final int TOO_LONG_QUEUE = 0;
        public static final int WAIT_TIME = 1;
    }
}
