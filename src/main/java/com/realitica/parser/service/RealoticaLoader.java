package com.realitica.parser.service;

import com.realitica.parser.entity.Stun;
import com.realitica.parser.repo.StunRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RealoticaLoader {

    @Value("${realitica.url:https://www.realitica.com/en}")
    private String REALITICA_URL;

    private List<String> CITIES = Arrays.asList();

    private final SimpleDateFormat SDF = new SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH);

    @Autowired
    StunRepository stunRepository;

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 6)
    public void loadFromRealitica() {
        log.info("Start scheduler loadFromRealitica");
        Map<String, Object> searchesByCitiesAndAreas
                = loadSearchesByCitiesAndAreas("https://www.realitica.com/rentals/Montenegro/", null);
        searchesByCitiesAndAreas = searchesByCitiesAndAreas.entrySet().stream()
                .filter(e -> CITIES.isEmpty() || CITIES.contains(e.getKey()))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
        List<String> searches = extractUrlsWithStuns(searchesByCitiesAndAreas);
        searches.forEach(urlWithStuns -> {
            log.info("Start to load by filter: {}", urlWithStuns);
            HashSet<String> ids = loadIdsBySearch(urlWithStuns);
            ids.stream().forEach(id -> saveStun(id, 1));
        });
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24 * 7)
    public void removeDeprecated() {
        log.info("Start scheduler removeDeprecated");
        OffsetDateTime deprecatedDate = OffsetDateTime.now().minusMonths(2);
        List<Stun> deprecatedStuns = stunRepository.findAll().stream()
                .filter(s -> s.getUpdated() == null || s.getUpdated().isBefore(deprecatedDate))
                .parallel()
                .filter(s -> {
                    Map<String, String> attributesMap = loadStunAttributes(s.getRealiticaId(), 1);
                    return attributesMap == null || attributesMap.isEmpty();
                })
                .collect(Collectors.toList());
        stunRepository.deleteAll(deprecatedStuns);
    }

    @SneakyThrows
    private Map<String, Object> loadSearchesByCitiesAndAreas(String rootUrl, String city) {
        Map<String, Object> searches = new LinkedHashMap<>();
        Document pageDoc = Jsoup.connect(rootUrl).get();
        Elements areasElements = pageDoc.select("#search_col2 span.geosel");

        if (city != null) {
            searches.put("All", "https://www.realitica.com/index.php?for=DuziNajam&lng=en&opa=" + city);
        }
        for (Element element : areasElements) {
            String current = element.text();
            if (StringUtils.isEmpty(current)) {
                continue;
            }

            current = current.split(" \\(")[0].trim().replace(" ", "+");

            String linkToChild = element.child(0).attr("href");
            if (element.child(0).childNodeSize() > 1 || current.equals("Budva")) {
                Map<String, Object> searchesInternal = loadSearchesByCitiesAndAreas(linkToChild, city == null ? current : city);
                searches.put(current, searchesInternal);
            } else {
                searches.put(current, "https://www.realitica.com/index.php?for=DuziNajam&lng=en&opa=" + city + "&cty=" + current);
            }
        }
        return searches;
    }

    private List<String> extractUrlsWithStuns(Map<String, Object> searches) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Object> e : searches.entrySet()) {
            if (e.getValue() instanceof String) {
                result.add((String) e.getValue());
            } else if (e.getValue() instanceof Map) {
                List<String> resultInternal = extractUrlsWithStuns((Map) e.getValue());
                result.addAll(resultInternal);
            }
        }
        return result;
    }

    @SneakyThrows
    private HashSet loadIdsBySearch(String urlWithStuns) {
        HashSet<String> ids = new LinkedHashSet<>();

        int curPage = 0;
        while (curPage >= 0) {
            try {
                String url = urlWithStuns + "&cur_page=" + curPage;
                Document pageDoc = Jsoup.connect(url).get();
                Elements stunElements = pageDoc.select("div.thumb_div > a");
                if (stunElements.size() == 0) {
                    log.info("Last page {}", curPage + 1);
                    curPage = -1;
                    continue;
                } else {
                    log.info("Loaded page {}", curPage + 1);
                    curPage++;
                }

                List<String> listIds = stunElements.stream()
                        .map(el -> el.attr("href"))
                        .filter(link -> link.startsWith("https://www.realitica.com/en/listing/"))
                        .map(link -> link.replace("https://www.realitica.com/en/listing/", ""))
                        .collect(Collectors.toList());
                ids.addAll(listIds);
            } catch (Throwable th) {
                log.error("Can't load page with stuns, goes to sleep", th);
                Thread.sleep(1000);
            }
        }
        return ids;
    }

    /**
     * attributesMap = {LinkedHashMap@7973}  size = 16
     * "Type" -> "Apartment Long Term Rental"
     * "District" -> "Podgorica"
     * "Location" -> "Masline"
     * "Address" -> "Masline, Podgorica, Crna Gora"
     * "Price" -> "€450"
     * "Bedrooms" -> "2"
     * "Living Area" -> "90 m"
     * "Description" -> "For rent a two bedroom apartment in house in Masline, area of 90m2, air conditioned.The yard is fenced and cultivated.Monthly rental price is € 450"
     * "More info at" -> ""
     * "Listed by" -> ""
     * "Registration number" -> "www.freshestate.me - facebook.com/FreshEstateMontenegro/"
     * "Mobile" -> "Mobitel Izdavanje Podgorica Alisa +382 69 355 898; Prodaja kuca i placeva Pg,Dg Zoran +382 69 274 699, sjever CG Darko +382 69 120 052, Primorje Miloš +382 69 022 070, Vladimir +382 69 355 886, office +382 69 223 514 - www.freshestate.me - www.Freshestate.me - We're multi language speaking stuff, contact us on @, Viber, WhatsApp, Facebook, Instagram and visit our offices and our site."
     * "Phone" -> "Telefon Prodaja stanova Podgorica Darko +382 69 120 052, Mirko +382 67 260 336; Aleksandar +382 69 372 006; Vladimir +382 69 372 007; Marijana + 382 69 372 009; Nikola +382 69 372 066; Tamara +382 69 372 116 ; Primorje Miloš +382 67 207 047, Vlado +382 67 260 391 ; office +382 69 223 514 - www.freshestate.me - We are multi language speaking stuff - Eng+382 67 207 047 - Tur+382 69 355 898 - Rus+382 69 355 886 - contact us on @, Viber, WhatsApp, Facebook, Instagram and visit our offices and our site."
     * "Listing ID" -> "2238224 (15098)"
     * "Last Modified" -> "6 Oct, 2020"
     * "Tags" -> ""
     *
     * @param id
     * @return
     * @throws IOException
     */
    @SneakyThrows
    public Map<String, String> loadStunAttributes(String id, int repeats) {
        if (repeats < 0) {
            return null;
        }

        try {
            log.info("Loading stun {}", id);

            Document doc = Jsoup.connect(REALITICA_URL + "/listing/" + id).get();
            Map<String, String> attributesMap = new LinkedHashMap();

            Elements parentElements = doc.select("div");
            for (Element parentElement : parentElements) {
                List<Node> nodes = parentElement.childNodes();
                for (int i = 0; i < nodes.size(); i++) {
                    Node node = nodes.get(i);
                    if (node instanceof Element) {
                        Element el = (Element) node;
                        if (Tag.valueOf("strong").equals(el.tag()) && i < nodes.size() - 1 && nodes.get(i + 1).toString().startsWith(": ")) {
                            attributesMap.put(el.text(), nodes.get(i + 1).toString().replace(":", "").trim());
                        }
                    }
                }
            }
            return attributesMap;
        } catch (Throwable th) {
            log.error("Can't load stun {}", id, th);
            Thread.sleep(1000);
            return loadStunAttributes(id, repeats - 1);
        }
    }

    private void saveStun(String id, int repeats) {
        try {
            if (repeats < 0) {
                log.error("Will be not repeat for {}", id);
                return;
            }

            Stun stun = stunRepository.findByRealiticaId(id);
            Map<String, String> attributesMap = loadStunAttributes(id, 1);
            if (attributesMap == null || attributesMap.isEmpty()) {
                if (stun != null) {
                    log.error("Attributes is empty for {}. Stun {} will be removed from DB", id, stun.getId());
                    stunRepository.delete(stun);
                }
                log.error("Attributes is empty for {}. Stun will be skipped, not founded in DB", id);
                return;
            }

            Date lastMobified = null;
            try {
                lastMobified = attributesMap.get("Last Modified") != null ? SDF.parse(attributesMap.get("Last Modified")) : null;
            } catch (ParseException e) {
                log.error("Can't parse data {}, {}", id, attributesMap.get("Last Modified"));
            }

            if (stun == null) {
                stun = new Stun();
                stun.setRealiticaId(id);
            }
            stun.setType(attributesMap.get("Type"));
            stun.setDistrict(attributesMap.get("District"));
            stun.setLocation(attributesMap.get("Location"));
            stun.setAddress(attributesMap.get("Address"));
            stun.setPrice(attributesMap.get("Price") != null ? attributesMap.get("Price").replace("€", "").replace(".", "") : null);
            stun.setBedrooms(attributesMap.get("Bedrooms"));
            stun.setLivingArea(attributesMap.get("Living Area"));
            stun.setMoreInfo(attributesMap.get("More info at"));
            stun.setLastModified(lastMobified);
            stun.setType(attributesMap.get("Type"));
            stun.setLink(REALITICA_URL + "/listing/" + id);
            stunRepository.save(stun);
            log.info("Save stun {}", id);
        } catch (Throwable th) {
            log.error("Can't save stun {}", id);
        }
    }
}
