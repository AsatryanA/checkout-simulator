package controller;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import object.Checkout;
import object.Customer;
import org.joda.time.DateTime;
import org.kordamp.ikonli.javafx.FontIcon;
import util.PropertiesTool;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SimulatorController extends Controller {
    public ImageView shelf1;
    public ImageView entry;
    public ImageView shelf2;
    public ScrollPane background;
    public FlowPane market;
    public Button startButton;
    public Button shutButton;
    public Button resetButton;
    public Slider playSpeed;
    public Label playSpeedDesc;

    private int playSpeedDivide;
    private boolean businessStatus;
    private Properties props;
    private int customerNo;
    private DateTime simulateTime;
    private ScheduledFuture<?> customerComingTask;
    private ScheduledFuture<?> timeCountTask;
    private ScheduledFuture<?> finishSimulationTask;

    public void initialize(URL location, ResourceBundle resources) {
        setMarketBackgroundAutoFit();
        props = PropertiesTool.getProps();
    }

    public void initSimulator() {
        initBtns();
        initBtnsEvent();

        businessStatus = false;
        model.pauseStatus = false;
        customerNo = 0;
        playSpeed.valueProperty().set(0);
        playSpeedDivide = 1;
        simulateTime = new DateTime().secondOfDay().setCopy(0);
        model.checkouts = new LinkedList<>();
        model.leftCustomers = new LinkedList<>();

        initCheckout();
        initCustomerComingService();
        if (finishSimulationTask != null) {
            finishSimulationTask.cancel(false);
        }
    }

    private void initTimeCountService() {
        int period = 1000000 / playSpeedDivide;
        int workingHours = Integer.parseInt(model.preferenceController.workingHours.getValue());
        if (timeCountTask != null) {
            timeCountTask.cancel(false);
        }
        timeCountTask = model.getThreadPoolExecutor().scheduleAtFixedRate(() -> {
            initTimeCountService();
            if (!model.pauseStatus) {
                simulateTime = simulateTime.plusSeconds(1);
                if (simulateTime.getHourOfDay() >= workingHours && finishSimulationTask == null) {
                    Platform.runLater(this::finishSimulation);
                }
                model.outputController.processBar.setProgress(simulateTime.getSecondOfDay() / 3600.0);
            }
        }, period, period, TimeUnit.MICROSECONDS);
    }

    private void initBtns() {
        startButton.setDisable(false);
        shutButton.setDisable(true);
        resetButton.setDisable(false);

        startButton.setGraphic(new FontIcon("fas-play"));
        startButton.setText("Սկսել");
        shutButton.setGraphic(new FontIcon("fas-stop"));
        shutButton.setText("Ավարտ");
        resetButton.setGraphic(new FontIcon("fas-reply"));
        resetButton.setText("Անցնել սկիզբ");
    }

    private void initCheckout() {
        market.getChildren().removeIf(node -> node.getClass() != HBox.class);
        model.checkouts.clear();

        int quantityOfCheckout = Integer.parseInt(props.getProperty(model.preferenceController.prefQuantityOfCheckouts.getId()));
        int no = 0;
        for (int i = 0; i < quantityOfCheckout; no++, i++) {
            Checkout channel = new Checkout(no + 1, Checkout.CheckoutType.NORMAL);
            market.getChildren().add(channel);
            model.checkouts.add(channel);
        }
        int quantityOfExpresswayCheckout = Integer.parseInt(props.getProperty(model.preferenceController.prefQuantityOfExpresswayCheckouts.getId()));
        for (int i = 0; i < quantityOfExpresswayCheckout; no++, i++) {
            Checkout channel = new Checkout(no + 1, Checkout.CheckoutType.EXPRESSWAY);
            market.getChildren().add(channel);
            model.checkouts.add(channel);
        }
        model.outputController.storeInitEvent();
    }

    private Checkout getBestCheckout(boolean isExpresswayAccessible) {
        int min = model.checkouts.stream()
                .filter(c -> isExpresswayAccessible || (c.getType() == Checkout.CheckoutType.NORMAL))
                .mapToInt(v -> v.getCustomers().size()).min().getAsInt();
        List<Checkout> chooseFromList = model.checkouts.stream()
                .filter(c -> isExpresswayAccessible || (c.getType() == Checkout.CheckoutType.NORMAL))
                .filter(c -> c.getCustomers().size() == min)
                .toList();
        int num = ThreadLocalRandom.current().nextInt(0, chooseFromList.size());

        return chooseFromList.get(num);
    }

    private void initBtnsEvent() {
        startButton.setOnAction(actionEvent -> {
            if (!businessStatus) {
                businessStatus = true;
                startButton.setGraphic(new FontIcon("fas-pause"));
                startButton.setText("Դադար");
                shutButton.setDisable(false);
                resetButton.setDisable(true);
                model.outputController.addLog("[ Սուպերմարկետ ] Բաց է ", Level.CONFIG);
                initTimeCountService();
                model.statisticsController.initStatisticsTask();
                return;
            }
            if (model.pauseStatus) {
                // to continue
                startButton.setGraphic(new FontIcon("fas-pause"));
                startButton.setText("Դադար");
                model.outputController.addLog("[ Սուպերմարկետ ] Շարունակել ", Level.CONFIG);
            } else {
                startButton.setGraphic(new FontIcon("fas-play"));
                startButton.setText("Շարունակել");
                model.outputController.addLog("[ Սուպերմարկետ ] Դադար ", Level.CONFIG);
            }
            model.pauseStatus = !model.pauseStatus;
        });

        shutButton.setOnAction(actionEvent -> {
            if (businessStatus) {
                finishSimulation();
            }
        });

        resetButton.setOnAction(actionEvent -> {
            model.shellController.setStep(0);
            model.shellController.stepTabPane.getSelectionModel().select(model.shellController.preferencesTab);
        });

        playSpeed.valueProperty().addListener((observableValue, number, newValue) -> {
            if (newValue.doubleValue() == 0) {
                playSpeedDivide = 1;
                playSpeedDesc.setText("1x");
            } else if (newValue.doubleValue() == 25) {
                playSpeedDivide = 2;
                playSpeedDesc.setText("2x");
            } else if (newValue.doubleValue() == 50) {
                playSpeedDivide = 4;
                playSpeedDesc.setText("4x");
            } else if (newValue.doubleValue() == 75) {
                playSpeedDivide = 8;
                playSpeedDesc.setText("8x");
            } else if (newValue.doubleValue() == 100) {
                playSpeedDivide = 16;
                playSpeedDesc.setText("16x");
            } else {
                return;
            }
            initCustomerComingService();
        });
    }

    private void finishSimulation() {
        startButton.setDisable(true);
        startButton.setGraphic(new FontIcon("fas-play"));
        startButton.setText("Սկսել");
        shutButton.setDisable(true);
        resetButton.setDisable(false);
        businessStatus = !businessStatus;
        customerComingTask.cancel(false);
        model.outputController.addLog("[ Սուպերմարկետ ] Փակել մուտքը, այսուհետ նոր հաճախորդներ չկան: ", Level.CONFIG);
        model.outputController.addLog("[ Սուպերմարկետ ] Ամբողջ հաշվետվության համար խնդրում ենք սպասել մինչև բոլոր հաճախորդները հեռանան: ", Level.CONFIG);
        finishSimulationTask = model.getThreadPoolExecutor().scheduleAtFixedRate(() -> {
            if (model.checkouts.stream().noneMatch(checkout -> checkout.getCounter().isBusying())) {
                finishSimulationTask.cancel(false);
                timeCountTask.cancel(false);
                model.statisticsController.completeStatisticsTask();

                model.outputController.addLog("[ Սուպերմարկետ ] Բոլոր հաճախորդները գնացել են: ", Level.CONFIG);
                model.outputController.addLog("[ Սուպերմարկետ ] Դռները փակ է։ ", Level.CONFIG);
                model.outputController.addLog("[ Սուպերմարկետ ] Սիմուլյացիան վերջացավ, շնորհակալություն", Level.CONFIG);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void initCustomerComingService() {
        int busyDegree = Double.valueOf(props.getProperty(model.preferenceController.prefBusyDegree.getId())).intValue();
        int period;
        if (busyDegree == 0) {
            period = 12000000 / playSpeedDivide;
        } else if (busyDegree == 25) {
            period = 6000000 / playSpeedDivide;
        } else if (busyDegree == 50) {
            period = 3000000 / playSpeedDivide;
        } else if (busyDegree == 75) {
            period = 1500000 / playSpeedDivide;
        } else if (busyDegree == 100) {
            period = 750000 / playSpeedDivide;
        } else {
            period = 3000000 / playSpeedDivide;
        }
        if (customerComingTask != null) {
            customerComingTask.cancel(false);
        }
        customerComingTask = model.getThreadPoolExecutor().scheduleAtFixedRate(() -> {
            if (businessStatus && !model.pauseStatus) {
                //quantity of goods
                int quantityFrom = Integer.parseInt(props.getProperty(model.preferenceController.prefRangeOfGoodsQuantityPerCustomerFrom.getId()));
                int quantityTo = Integer.parseInt(props.getProperty(model.preferenceController.prefRangeOfGoodsQuantityPerCustomerTo.getId()));
                int quantity = ThreadLocalRandom.current().nextInt(quantityFrom, quantityTo + 1);
                //temper
                double temper = Math.random();
                double temperDivide = 0;
                try {
                    temperDivide = new DecimalFormat("0.0#%").parse(props.getProperty(model.preferenceController.prefPercentageOfACustomerWhoCantWait.getId())).doubleValue();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                boolean cannotWait = temper < temperDivide;
                String minsText = props.getProperty(model.preferenceController.prefCustomerWillLeaveAfterWaitingFor.getId());
                String mins = minsText.replaceAll("mins", "");
                int waitSec = Integer.parseInt(mins) * 60;

                //choose the best checkout
                int lessThan = Integer.parseInt(props.getProperty(model.preferenceController.prefExpresswayCheckoutsForProductsLessThan.getId()));
                Checkout bestCheckout = getBestCheckout(quantity <= lessThan);

                Customer customer = new Customer(++customerNo, quantity, cannotWait, waitSec, bestCheckout);
                bestCheckout.addCustomer(customer);
            }
        }, period, period, TimeUnit.MICROSECONDS);
    }

    private void setMarketBackgroundAutoFit() {
        background.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
            shelf1.setFitWidth(newValue.getWidth() * 100.0 / 381);
            entry.setFitWidth(newValue.getWidth() * 181.0 / 381);
            shelf2.setFitWidth(newValue.getWidth() * 100.0 / 381);
        });
    }

    public int getPlaySpeedDivide() {
        return playSpeedDivide;
    }

    public DateTime getSimulateTime() {
        return simulateTime;
    }
}
