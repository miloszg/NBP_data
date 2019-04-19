package pl.parser.nbp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static java.lang.System.exit;

class MainClass {
    static float buy_value=0;
    static float sale_value=0;
    static float daysBetween=0.0f;
    static ArrayList<Float> saleValuesArray=new ArrayList<>();

    public static void main(String[] args) throws ParserConfigurationException {
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Starting params - CODE CURRENCY
        currencyCodeValidation(args[0]);
        String currencyCode=args[0];

        // Starting params - STARTING DATE
        dateValidation(args[1]);
        LocalDate startingDate=LocalDate.parse(args[1],formatter);

        // Starting params - ENDING DATE
        dateValidation(args[2]);
        LocalDate endingDate=LocalDate.parse(args[2],formatter);

        //Days between Starting and Ending Date
        daysBetween = ChronoUnit.DAYS.between(startingDate, endingDate)+1;

        // Looping through dates between given period of time
        for(int i=0;i<daysBetween;i++) {
            getXmlTableData(getXmlTableCode(startingDate),currencyCode);
            startingDate=startingDate.plusDays(1);
        }

        float sum=0;
        for(int k=0;k<saleValuesArray.size();k++){
            sum+=Math.pow((saleValuesArray.get(k)-(sale_value/daysBetween)),2);
        }
        System.out.println("Average currency buy rate:          "+String.format("%.4f",buy_value/daysBetween));
        System.out.println("Standard deviation of sales rates:  "+String.format("%.4f",Math.sqrt(sum/daysBetween)));
    }

    /** Getting dir.txt with xml table names*/
    private static String getXmlTableCode(LocalDate startingDate) {
        String xmlCode = "";
        try {
            String url_text = "http://www.nbp.pl/kursy/xml/dir" + startingDate.getYear() + ".txt";
            URL url = new URL(url_text);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            int year=startingDate.getYear() - 2000;
            String regex_text = "c...z" + String.format("%02d",year) + String.format("%02d", startingDate.getMonthValue()) + String.format("%02d", startingDate.getDayOfMonth());
            while ((xmlCode = in.readLine()) != null) {
                if (Pattern.matches(regex_text, xmlCode)) {
                    break;
                }
            }
            in.close();
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        return xmlCode;
    }

    /**Reading xml data and parsing*/
    private static void getXmlTableData(String xmlCode,String currencyCode) throws ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        try {
            URL url = new URL("http://www.nbp.pl/kursy/xml/" + xmlCode + ".xml");
            InputStream stream = url.openStream();
            Document doc = builder.parse(stream);
            doc.normalize();

            NodeList rootNodes = doc.getElementsByTagName("tabela_kursow");
            Node rootNode = rootNodes.item(0);
            Element rootElement = (Element) rootNode;
            NodeList rootList = rootElement.getElementsByTagName("pozycja");
            for (int j = 0; j < rootList.getLength(); j++) {
                Node currency = rootList.item(j);
                Element currencyElement = (Element) currency;
                String xmlCurrencyCode = ((Element) currency).getElementsByTagName("kod_waluty").item(0).getTextContent();

                if (xmlCurrencyCode.equals(currencyCode.toUpperCase())) {
                    String stringBuyRate = currencyElement.getElementsByTagName("kurs_kupna").item(0).getTextContent().replace(",", ".");
                    buy_value += Float.parseFloat(stringBuyRate);
                    String stringSaleRate = currencyElement.getElementsByTagName("kurs_sprzedazy").item(0).getTextContent().replace(",", ".");
                    saleValuesArray.add(Float.parseFloat(stringSaleRate));
                    sale_value += Float.parseFloat(stringSaleRate);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            daysBetween--;
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    /** Currency code validation*/
    private static void currencyCodeValidation(String currencyCode) {
        String[] CURRENCIES={"USD","EUR","CHF","GBP"};
        List<String> listCurrency=Arrays.asList(CURRENCIES);
        if(!listCurrency.contains(currencyCode.toUpperCase())) {
            System.out.println("Wrong currency code! Program will shutdown!");
            exit(1);
        }
    }

    /** Date input validation*/
    private static void dateValidation(String date){
        String[] parts = date.split("-");
        if(!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println("Wrong date format! Program will shutdown!");
            exit(1);
        }
        if((Integer.valueOf(parts[0])<2000 || Integer.valueOf(parts[0])>2019) || (Integer.valueOf(parts[1])>12 || Integer.valueOf(parts[1])==0 ) || (Integer.valueOf(parts[2])>31 || Integer.valueOf(parts[2])==0)){
            System.out.println("Wrong date! Please enter correct date. Program will shutdown!");
            exit(1);
        }
    }
}
