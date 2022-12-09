package util;

import database.Database;
import org.glassfish.grizzly.utils.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class HtmlUtils {
    private static final String LINK_PART_ONE = "https://www.myhome.ge/en/s/Apartment-for-rent-Tbilisi" +
            "?Keyword=Tbilisi&AdTypeID=3&PrTypeID=1&Page=";
    private static final String LINK_PART_TWO = "&cities=1996871&GID=1996871&FCurrencyID=1&FPriceTo=900" +
            "&AreaSizeFrom=50&FloorNums=notfirst";
    private static final int MAX_PHOTO_COUNT = 11;

    public static ArrayList<Pair<String, ArrayList<String>>> parse(String html) {
        ArrayList<Pair<String, ArrayList<String>>> descList = new ArrayList<>();
        try {
            Document document = Jsoup.connect(html).get();
            // maybe better to get statement-card?
            Elements searchContents = document.getElementsByClass("card-container");
            searchContents.forEach((Element apartment) -> {
                String url = apartment.attr("href");
                int id = Integer.parseInt(url.substring(28, 36)); //todo change later
                String address = apartment.getElementsByClass("address").attr("title");

                var swiper = apartment.getElementsByClass("swiper-lazy");
                int photoCount = Integer.parseInt(swiper.attr("data-photos-cnt"));
                ArrayList<String> pictures = new ArrayList<>();
                if (photoCount > 0) {
                    for (int i = 1; i < Math.min(photoCount, MAX_PHOTO_COUNT); i++) {
                        String photo_url = swiper.attr("data-src")
                                .replace("thumbs", "large").substring(0, 60) + i + ".jpg";
                        pictures.add(photo_url);
                    }
                }

                Database.getInstance().connect();
                if (isDidiDigomi(address) && !Database.getInstance().checkIfExists(id)) {
                    //todo add phone
                    String phone = "";
                    try {
                        Document flat = Jsoup.connect(url).get();
                        phone = flat.getElementsByClass("statement-author").attr("href");
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    Database.getInstance().insertData(id);
                    System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
                            + "]" + " Insert success for id: " + id);
                    descList.add(new Pair<>(url + '\n' +
                            "Адрес: " + address + '\n' +
                            "Цена: " + apartment.getElementsByClass("item-price-usd").get(0).text() + "$" + "\n" +
                            "Площадь: " + apartment.getElementsByClass("item-size").get(0).text().split("\\.")[0] + "m²" + "\n" +
                            "Этаж: " + apartment.getElementsByClass("options-texts").get(0).text().substring(6) + ", " +
                            "комнат: " + apartment.getElementsByClass("options-texts").get(1).text().substring(5), //+ "\n" +
                            //"Телeфон: " + phone,
                            pictures));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return descList;
    }

    private static boolean isDidiDigomi(String address) {
        return !address.contains("Didi Digomi");
    }

    public static String generatePage(int pageNumber) {
        return LINK_PART_ONE + pageNumber + LINK_PART_TWO;
    }
}
