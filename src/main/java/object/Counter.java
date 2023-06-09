package object;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import model.MainModel;
import org.joda.time.DateTime;
import util.PropertiesTool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Counter extends StackPane {
    private final Circle counterStatusCircle;
    private final Tooltip tooltip;

    private final int no;
    private final boolean status;
    private boolean busying;
    private final int type;
    private int totalServedCustomers;
    private final Map<Integer, Integer> totalServedProducts;
    private DateTime totalServedTime;
    private ScheduledFuture<?> tooltipUpdateTask;
    private ScheduledFuture<?> timeCountTask;
    private ScheduledFuture<?> scanTask;

    public Counter(int no, int type, boolean status) {
        this.no = no;
        this.status = status;
        this.type = type;
        totalServedCustomers = 0;

        totalServedTime = new DateTime().secondOfDay().setCopy(0);
        totalServedProducts = new HashMap<>();
        initScanService(1000000);

        ImageView counterImageView = new ImageView();
        counterImageView.setFitHeight(150);
        counterImageView.setFitWidth(200);
        counterImageView.setPickOnBounds(true);
        counterImageView.setPreserveRatio(true);
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/counter.png")));
        counterImageView.setImage(image);

        tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setStyle("-fx-font-weight: bold");
        setOnMouseMoved(mouseEvent -> {
            initTooltipService(500);
            tooltip.show((Node) mouseEvent.getSource(), mouseEvent.getScreenX() + 5, mouseEvent.getScreenY() + 15);
        });
        setOnMouseExited(mouseEvent -> {
            initTooltipService(0);
            tooltip.hide();
        });

        counterStatusCircle = new Circle();
        counterStatusCircle.setStrokeWidth(25.0);
        counterStatusCircle.setStroke(Paint.valueOf("limegreen"));
        StackPane.setAlignment(counterStatusCircle, Pos.TOP_RIGHT);

        Label label = new Label();
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        label.setStyle("-fx-font-style: italic;-fx-font-weight: bold;" +
                "-fx-font-size: 16;" + "-fx-font-family: 'Arial AMU'");
        label.setFont(new Font("Arial AMU", 16));

        if (type == Checkout.CheckoutType.EXPRESSWAY) {
            label.setText("ԷՔՍՊՐԵՍ");
        } else {
            label.setText(String.valueOf(no));
        }
        getChildren().addAll(counterImageView, counterStatusCircle, label);
    }

    private void initScanService(int scanItemInterval) {
        if (scanTask != null) {
            scanTask.cancel(false);
        }

        int playSpeedDivide = MainModel.getInstance().simulatorController.getPlaySpeedDivide();
        int period = scanItemInterval / playSpeedDivide;
        scanTask = MainModel.getInstance().getThreadPoolExecutor().scheduleAtFixedRate(() -> {
            if (!MainModel.getInstance().pauseStatus) {
                Checkout channel = ((Checkout) getParent());
                if (channel.getCustomers().size() > 0) {
                    setBusying(true);
                    Customer nowCustomer = channel.getCustomers().peek();
                    ((Checkout) getParent()).getCustomerQueue().setNowCustomer(nowCustomer);
                    assert nowCustomer != null;
                    nowCustomer.setBeingServed(true);

                    int waitFor = nowCustomer.getQuantityWaitForCheckout();

                    // scan goods
                    double from = Double.parseDouble(PropertiesTool.getProps().getProperty(MainModel.getInstance().preferenceController.prefRangeOfEachProductScanTimeFrom.getId()));
                    double to = Double.parseDouble(PropertiesTool.getProps().getProperty(MainModel.getInstance().preferenceController.prefRangeOfEachProductScanTimeTo.getId()));
                    double v = ThreadLocalRandom.current().nextDouble(from, to);
                    initScanService((int) (v * 1000000));

                    int minute = MainModel.getInstance().simulatorController.getSimulateTime().getMinuteOfDay() + 1;
                    if (totalServedProducts.containsKey(minute)) {
                        totalServedProducts.compute(minute, (integer, integer2) -> integer2 + 1);
                    } else {
                        totalServedProducts.put(minute, 1);
                    }
                    nowCustomer.setQuantityWaitForCheckout(--waitFor);

                    // change the arc
                    double l = 360.0 * nowCustomer.getQuantityWaitForCheckout() / nowCustomer.getQuantityOfGoods();
                    ((Checkout) getParent()).getCustomerQueue().updateArc(l);

                    // if 0, delete
                    if (waitFor == 0) {
                        totalServedCustomers += 1;
                        Customer customer = channel.getCustomers().poll();
                        assert customer != null;
                        ((Checkout) getParent()).leaveCustomer(customer, true);
                    }
                } else {
                    setBusying(false);
                    initScanService(1000000);
                }
            }
        }, period, period, TimeUnit.MICROSECONDS);
    }

    public void setBusying(boolean busyingStatus) {
        if (busyingStatus == busying) {
            return;
        }
        busying = busyingStatus;
        if (busyingStatus) {
            counterStatusCircle.setStroke(Paint.valueOf("#eae600"));
            initTimeCountService();
        } else {
            counterStatusCircle.setStroke(Paint.valueOf("limegreen"));
        }
    }

    public void initTimeCountService() {
        if (timeCountTask != null) {
            timeCountTask.cancel(false);
        }
        if (!busying) {
            return;
        }
        int playSpeedDivide = MainModel.getInstance().simulatorController.getPlaySpeedDivide();
        if (playSpeedDivide != 0) {
            int period = 1000000 / playSpeedDivide;
            timeCountTask = MainModel.getInstance().getThreadPoolExecutor().scheduleAtFixedRate(() -> {
                initTimeCountService();
                if (!MainModel.getInstance().pauseStatus) {
                    totalServedTime = totalServedTime.plusSeconds(1);
                }
            }, period, period, TimeUnit.MICROSECONDS);
        }
    }

    public void initTooltipService(long period) {
        if (tooltipUpdateTask != null) {
            tooltipUpdateTask.cancel(false);
        }
        if (period == 0) {
            return;
        }
        tooltipUpdateTask = MainModel.getInstance().getThreadPoolExecutor().scheduleAtFixedRate(() -> Platform.runLater(() -> tooltip.setText("checkout " + no + "\n\n" +
                "status: " + (status ? "busying" : "idle") + "\n" +
                "served customers: " + totalServedCustomers + "\n" +
                "valid time: " + totalServedTime.getSecondOfDay() + "s\n" +
                "type: " + (type == Checkout.CheckoutType.NORMAL ? "normal" : "expressway"))), 0, period, TimeUnit.MILLISECONDS);
    }

    public int getNo() {
        return no;
    }

    public boolean isBusying() {
        return busying;
    }

    public DateTime getTotalServedTime() {
        return totalServedTime;
    }

    public Map<Integer, Integer> getTotalServedProducts() {
        return totalServedProducts;
    }
}
