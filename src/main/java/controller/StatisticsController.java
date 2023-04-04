package controller;

import application.MainApp;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Side;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import object.Customer;
import object.TextAreaExpandable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import util.PropertiesTool;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StatisticsController extends Controller {
    public PieChart waitTimeDistributionPie;
    public ScatterChart waitTimeEachCustomerScatter;
    public BarChart utilizationEachCheckoutBar;
    public ScrollPane ReportPane;
    public TextField extendOfBusy;
    public TextField rangeOfGoodsQuantity;
    public TextField percentageOfACustomerCanNotWait;
    public TextField CustomerWillLeaveAfter;
    public TextField quantityOfNormalCheckouts;
    public TextField quantityOfExpresswayCheckouts;
    public TextField ExpresswayCheckoutsFor;
    public TextField rangeOfScanTime;
    public TextField date;
    public LineChart totalProductionProcessedLine;
    public TextAreaExpandable recordDetail;
    public VBox VBox;
    public Button exportButton;

    private final Properties props = PropertiesTool.getProps();
    private ObservableList<PieChart.Data> waitTimeDistributionPieData;
    private Map<String, Integer> waitTimeDistributionMap;
    private ScheduledFuture<?> timeTask;
    private int hasProcessedCustomers;
    private Map<String, Integer> hasProcessedProductsMinute;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        waitTimeDistributionPie.setTitle("Սպասման ժամանակահատվածի բաշխումը");
        waitTimeDistributionPie.setLegendSide(Side.RIGHT);
        waitTimeDistributionPieData = waitTimeDistributionPie.getData();
        waitTimeDistributionMap = new HashMap<>();

        waitTimeEachCustomerScatter.setTitle("Յուրաքանչյուր հաճախորդի սպասման ժամանակը");
        waitTimeEachCustomerScatter.getXAxis().setLabel("Հաճախորդ (հ․)");
        waitTimeEachCustomerScatter.getYAxis().setLabel("սպասել ժամանակ(վ)");
        waitTimeEachCustomerScatter.setLegendSide(Side.RIGHT);

        utilizationEachCheckoutBar.setTitle("Յուրաքանչյուր դրամարկղի օգտագործումը");
        utilizationEachCheckoutBar.getXAxis().setLabel("Դրամարկղ (հ․)");
        utilizationEachCheckoutBar.getYAxis().setLabel("օգտագործումը(%)");
        utilizationEachCheckoutBar.setLegendSide(Side.RIGHT);

        totalProductionProcessedLine.setTitle("Րոպեում մշակված ապրանքներ");
        totalProductionProcessedLine.getXAxis().setLabel("Րոպե");
        totalProductionProcessedLine.getYAxis().setLabel("Ապրանքներ (քանակ)");
        totalProductionProcessedLine.setLegendSide(Side.RIGHT);

        recordDetail = new TextAreaExpandable();
        VBox.getChildren().add(recordDetail);

        exportButton.setOnAction(event -> {
            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(ReportPane.getContent().snapshot(new SnapshotParameters(), null), null);
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialFileName("SimulationReport.pdf");
                fileChooser.setTitle("choose path");
                File file = fileChooser.showSaveDialog(MainApp.getPrimaryStage());
                FileOutputStream out = new FileOutputStream(file);
                javax.imageio.ImageIO.write(bufferedImage, "png", out);
                out.flush();
                out.close();

                com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(file.getAbsolutePath());
                image.scalePercent(100);
                Document doc = new Document(new com.itextpdf.text.Rectangle(image.getScaledWidth(), image.getScaledHeight()));
                FileOutputStream fos = new FileOutputStream(file);
                PdfWriter.getInstance(doc, fos);
                doc.open();
                doc.newPage();
                image.setAbsolutePosition(0, 0);
                doc.add(image);
                fos.flush();
                doc.close();
                fos.close();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "save successful!", ButtonType.YES);
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "save fails!", ButtonType.YES);
                alert.showAndWait();
            }

        });
    }

    public void initStatistics(List<String> names) {
        String extend = props.getProperty(model.preferenceController.prefBusyDegree.getId());
        String leaveAfter = props.getProperty(model.preferenceController.prefCustomerWillLeaveAfterWaitingFor.getId());
        String expresswayForLessThan = props.getProperty(model.preferenceController.prefExpresswayCheckoutsForProductsLessThan.getId());
        String cantWait = props.getProperty(model.preferenceController.prefPercentageOfACustomerWhoCantWait.getId());
        String checkouts = props.getProperty(model.preferenceController.prefQuantityOfCheckouts.getId());
        String expresswayCheckouts = props.getProperty(model.preferenceController.prefQuantityOfExpresswayCheckouts.getId());
        String scanFrom = props.getProperty(model.preferenceController.prefRangeOfEachProductScanTimeFrom.getId());
        String scanTo = props.getProperty(model.preferenceController.prefRangeOfEachProductScanTimeTo.getId());
        String goodsFrom = props.getProperty(model.preferenceController.prefRangeOfGoodsQuantityPerCustomerFrom.getId());
        String goodsTo = props.getProperty(model.preferenceController.prefRangeOfGoodsQuantityPerCustomerTo.getId());
        extendOfBusy.setText(MessageFormat.format("Հաճախորդների քանակը *: {0}", extend));
        rangeOfGoodsQuantity.setText(MessageFormat.format("Ապրանքների քանակի շրջանակը:  {0} -ից մինչև {1}", goodsFrom, goodsTo));
        percentageOfACustomerCanNotWait.setText(MessageFormat.format("Հաճախորդի տոկոսը, ով չի կարող սպասել: {0}", cantWait));
        CustomerWillLeaveAfter.setText(MessageFormat.format("Հաճախորդը կհեռանա սպասելուց հետո: {0}", leaveAfter));
        quantityOfNormalCheckouts.setText(MessageFormat.format("Սովորական վճարումների քանակը: {0}", checkouts));
        quantityOfExpresswayCheckouts.setText(MessageFormat.format("Էքսպրես դրամարկղերի քանակը: {0}", expresswayCheckouts));
        ExpresswayCheckoutsFor.setText(MessageFormat.format("Էքսպրես դրամարկղեր ավելի քիչ ապրանքների համար քան {0}", expresswayForLessThan));
        rangeOfScanTime.setText(MessageFormat.format("Յուրաքանչյուր ապրանքի սկանավորման ժամանակի շրջանակը: {0} -ից մինչև {1}", scanFrom, scanTo));
        date.setText("ամսաթիվը: " + DateTimeFormat.forPattern("EEEE, dd MMMM yyyy (ZZZ)").print(new DateTime()));
        ReportPane.setVvalue(0.3);
        initWaitTimeDistributionPie();
        initWaitTimeEachCustomerScatter(names);
        initUtilizationEachCheckoutBar(names);
        initTotalProductionProcessedLine(names);

        hasProcessedCustomers = 0;
        hasProcessedProductsMinute = names.stream().collect(Collectors.toMap(s -> s, s -> 1));
        recordDetail.setText("հասանելի է սիմուլյացիայի ավարտից հետո...");
        exportButton.setDisable(true);
    }

    public void initStatisticsTask() {
        if (timeTask != null) {
            timeTask.cancel(false);
        }
        date.setText(date.getText() + "   սկիզբ: " + DateTimeFormat.forPattern("HH:mm:ss").print(new DateTime()));
        timeTask = model.getThreadPoolExecutor().scheduleAtFixedRate(() -> {
            if (!model.pauseStatus) {
                String waitSecPeriod;
                for (int i = hasProcessedCustomers; i < model.leftCustomers.size(); i++) {
                    Customer customer = model.leftCustomers.get(i);
                    if (customer.isCannotWait() && (customer.getWaitSecActual() >= customer.getWaitSec())) {
                        waitSecPeriod = "հեռանալ";
                    } else if (customer.getWaitSecActual() <= 60) {
                        waitSecPeriod = "<1min";
                    } else if (customer.getWaitSecActual() <= 300) {
                        waitSecPeriod = "1-5min";
                    } else if (customer.getWaitSecActual() <= 600) {
                        waitSecPeriod = "5-10min";
                    } else if (customer.getWaitSecActual() <= 900) {
                        waitSecPeriod = "10-15min";
                    } else if (customer.getWaitSecActual() <= 1200) {
                        waitSecPeriod = "15-20min";
                    } else {
                        waitSecPeriod = ">20min";
                    }
                    updateWaitTimeDistributionPie(waitSecPeriod);
                    updateWaitTimeEachCustomerScatter(customer.parent.getCounter().getNo(), customer.getNo(), customer.getWaitSecActual());
                }
                model.checkouts.forEach(checkout -> {
                    double v = 100.0 * checkout.getCounter().getTotalServedTime().getSecondOfDay() / model.simulatorController.getSimulateTime().getSecondOfDay();
                    updateUtilizationEachCheckoutBar(checkout.getCounter().getNo(), v);

                    Map<Integer, Integer> totalServed = checkout.getCounter().getTotalServedProducts();
                    int minute = model.simulatorController.getSimulateTime().getMinuteOfDay() + 1;

                    Integer lastTimeMinute = hasProcessedProductsMinute.get("դրամարկղ" + checkout.getCounter().getNo());

                    if (minute != lastTimeMinute) {
                        updateTotalProductionProcessedLine(checkout.getCounter().getNo(), minute - 1, totalServed.getOrDefault(minute - 1, 0));
                        hasProcessedProductsMinute.put("դրամարկղ" + checkout.getCounter().getNo(), minute);
                    } else {
                        updateTotalProductionProcessedLine(checkout.getCounter().getNo(), minute, totalServed.getOrDefault(minute, 0));
                    }
                });
                hasProcessedCustomers = model.leftCustomers.size();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void completeStatisticsTask() {
        exportButton.setDisable(false);
        if (timeTask != null) {
            timeTask.cancel(false);
        }
        date.setText(date.getText() + "   ավարտ: " + DateTimeFormat.forPattern("HH:mm:ss").print(new DateTime()));
        StringBuilder detail = new StringBuilder("Ընդամենը\n" +
                "Հաճախորդներ: " + model.leftCustomers.size() + "\n" +
                "սպասել ավելի քիչ, քան 1min: " + waitTimeDistributionMap.get("<1min") + "\t\t\t" +
                "սպասել 1-5mins: " + waitTimeDistributionMap.get("<1min") + "\t\t\t\t" +
                "սպասել 5-10mins: " + waitTimeDistributionMap.get("5-10min") + "\n" +
                "սպասել 10-15mins: " + waitTimeDistributionMap.get("10-15min") + "\t\t\t\t" +
                "սպասել 15-20mins: " + waitTimeDistributionMap.get("15-20min") + "\t\t\t" +
                "սպասել ավելին, քան 20mins: " + waitTimeDistributionMap.get(">20min") + "\n" +
                "կորցրած հաճախորդներ: " + waitTimeDistributionMap.get("հեռանալ") + "\t\t\t\t\t" +
                "վաճառվող ապրանքներ: " + model.checkouts.stream().mapToInt(value -> value.getCounter().getTotalServedProducts().values().stream().mapToInt(Integer::intValue).sum()).sum() + "\n");
        DecimalFormat format = new DecimalFormat("#0.00");
        detail.append("\n\nմիջին\n" +
                "հաճախորդի սպասման ժամանակը: " + format.format(model.leftCustomers.stream().mapToInt(Customer::getWaitSecActual).average().orElse(0)) + "\t\t\t" +
                "հաճախորդի սպասման ժամանակ (բացառությամբ․․․ ): " + format.format(model.leftCustomers.stream().filter(customer -> !(customer.isCannotWait() && (customer.getWaitSecActual() >= customer.getWaitSec()))).mapToInt(Customer::getWaitSecActual).average().orElse(0)) + "\n" +
                "դրամարկղի օգտագործումը: " + format.format(model.checkouts.stream().mapToDouble(value -> 100.0 * value.getCounter().getTotalServedTime().getSecondOfDay() / model.simulatorController.getSimulateTime().getSecondOfDay()).average().orElse(0)) + "%\t\t\t" +
                "ապրանքներ մեկ զամբյուղի համար: " + format.format(model.leftCustomers.stream().mapToInt(Customer::getQuantityOfGoods).average().orElse(0)) + "\n");
        model.checkouts.stream().forEach(checkout -> {
            detail.append("դրամարկղ" + checkout.getCounter().getNo() + " օգտագործումը: ");
            double v = 100.0 * checkout.getCounter().getTotalServedTime().getSecondOfDay() / model.simulatorController.getSimulateTime().getSecondOfDay();
            detail.append(format.format(v) + "%\n");
        });

        recordDetail.setText(detail.toString());
    }

    public void initWaitTimeEachCustomerScatter(List<String> names) {
        waitTimeEachCustomerScatter.getData().setAll(names
                .stream().map(name -> {
                    XYChart.Series series = new XYChart.Series();
                    series.setName(name);
                    series.getData().add(new XYChart.Data<>(0, 0));
                    return series;
                }).collect(Collectors.toList()));
        waitTimeEachCustomerScatter.getData().forEach(o -> ((XYChart.Series) o).getData().clear());
    }

    public void updateWaitTimeEachCustomerScatter(int checkoutNo, int customerNo, int sec) {
        waitTimeEachCustomerScatter.getData().stream()
                .filter(o -> ((XYChart.Series) o).getName().equals("դրամարկղ" + checkoutNo))
                .findFirst().ifPresent(o -> Platform.runLater(() -> {
            ((XYChart.Series) o).getData().add(new XYChart.Data<>(customerNo, sec));
        }));
    }

    public void initUtilizationEachCheckoutBar(List<String> names) {
        XYChart.Series series = new XYChart.Series();
        series.setName("օգտագործումը");
        series.getData().setAll(names
                .stream().map(name -> {
                    XYChart.Data<String, Double> data = new XYChart.Data<>();
                    data.setXValue(name);
                    data.setYValue(0.0);
                    return data;
                }).collect(Collectors.toList()));
        utilizationEachCheckoutBar.getData().add(series);
    }

    public void updateUtilizationEachCheckoutBar(int checkoutNo, double percent) {
        utilizationEachCheckoutBar.getData().stream()
                .findFirst().ifPresent(o -> Platform.runLater(() -> {
            ((XYChart.Data) ((XYChart.Series) o).getData().stream().filter(o1 -> ((XYChart.Data) o1).getXValue().equals("դրամարկղ" + checkoutNo)).findFirst().get()).setYValue(percent);
        }));
    }

    public void initWaitTimeDistributionPie() {
        waitTimeDistributionPieData.setAll(
                new PieChart.Data("<1min", 100),
                new PieChart.Data("1-5min", 0),
                new PieChart.Data("5-10min", 0),
                new PieChart.Data("10-15min", 0),
                new PieChart.Data("15-20min", 0),
                new PieChart.Data(">20min", 0),
                new PieChart.Data("leave", 0));
        waitTimeDistributionMap.put("<1min", 0);
        waitTimeDistributionMap.put("1-5min", 0);
        waitTimeDistributionMap.put("5-10min", 0);
        waitTimeDistributionMap.put("10-15min", 0);
        waitTimeDistributionMap.put("15-20min", 0);
        waitTimeDistributionMap.put(">20min", 0);
        waitTimeDistributionMap.put("leave", 0);
    }

    public void updateWaitTimeDistributionPie(String key) {
        waitTimeDistributionMap.put(key, waitTimeDistributionMap.get(key) + 1);
        int total = waitTimeDistributionMap.values().stream().mapToInt(Integer::intValue).sum();
        for (PieChart.Data d : waitTimeDistributionPieData) {
            double v = 100.0 * waitTimeDistributionMap.get(d.getName()) / total;
            Platform.runLater(() -> d.setPieValue(v));
        }
    }

    public void initTotalProductionProcessedLine(List<String> names) {
        totalProductionProcessedLine.getData().setAll(names
                .stream().map(name -> {
                    XYChart.Series series = new XYChart.Series();
                    series.setName(name);
                    return series;
                }).collect(Collectors.toList()));
    }

    public void updateTotalProductionProcessedLine(int checkoutNo, int minute, int quantity) {
        totalProductionProcessedLine.getData().stream()
                .filter(series -> ((XYChart.Series) series).getName().equals("դրամարկղ" + checkoutNo))
                .findFirst().ifPresent(series -> ((XYChart.Series) series).getData()
                .stream().filter(data -> (Integer) ((XYChart.Data) data).getXValue() == minute)
                .findFirst().ifPresentOrElse(data -> Platform.runLater(() -> {
                    ((XYChart.Data) data).setYValue(quantity);
                }), () -> Platform.runLater(() -> {
                    ((XYChart.Series) series).getData().add(new XYChart.Data<>(minute, quantity));
                })))
        ;
    }
}
