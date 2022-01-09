package com.realitica.parser.service;

import com.realitica.parser.entity.AdEntity;
import com.realitica.parser.repo.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
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
@RequiredArgsConstructor
public class RealiticaLoader {

    private final AdRepository adRepository;

    @Value("${realitica.url:https://www.realitica.com}")
    private String realiticaUrl;
    private List<String> CITIES_FILTER = List.of();
    private final static SimpleDateFormat SDF = new SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH);

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 6)
    public void loadFromRealitica() {
        log.info("Start scheduler loadFromRealitica");
        var searchesByCitiesAndAreas
                = loadSearchesByCitiesAndAreas(realiticaUrl + "/rentals/Montenegro/", null);
        searchesByCitiesAndAreas = searchesByCitiesAndAreas.entrySet().stream()
                .filter(e -> CITIES_FILTER.isEmpty() || CITIES_FILTER.contains(e.getKey()))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
        var searches = extractFlatSearchUrlList(searchesByCitiesAndAreas);
        searches.forEach(urlWithAd -> {
            log.info("Start to load by filter: {}", urlWithAd);
            var ids = loadIdsBySearch(urlWithAd);
            ids.stream().forEach(id -> loadAdAndSave(id, 1));
        });
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24 * 7)
    public void removeDeprecated() {
        log.info("Start scheduler removeDeprecated");
        var deprecatedDate = OffsetDateTime.now().minusMonths(2);
        var deprecatedAdEntities = adRepository.findAll().stream()
                .filter(s -> s.getUpdated() == null || s.getUpdated().isBefore(deprecatedDate))
                .parallel()
                .filter(s -> {
                    Map<String, String> attributesMap = loadAdAttributes(s.getRealiticaId(), 1);
                    return attributesMap == null || attributesMap.isEmpty();
                })
                .collect(Collectors.toList());
        adRepository.deleteAll(deprecatedAdEntities);
    }

    /**
     * Recursively collect urls of search urls
     *
     * @param rootUrl
     * @param city
     * @return
     */
    @SneakyThrows
    private Map<String, Object> loadSearchesByCitiesAndAreas(String rootUrl, String city) {
        var searches = new LinkedHashMap<String, Object>();
        var pageDoc = Jsoup.connect(rootUrl).get();
        var areasElements = pageDoc.select("#search_col2 span.geosel");

        if (city != null) {
            searches.put("All", "https://www.realitica.com/index.php?for=DuziNajam&lng=en&opa=" + city);
        }
        for (var element : areasElements) {
            String current = element.text();
            if (StringUtils.isEmpty(current)) {
                continue;
            }

            current = current.split(" \\(")[0].trim().replace(" ", "+");

            var linkToChild = element.child(0).attr("href");
            if (element.child(0).childNodeSize() > 1 || current.equals("Budva")) {
                Map<String, Object> searchesInternal = loadSearchesByCitiesAndAreas(linkToChild, city == null ? current : city);
                searches.put(current, searchesInternal);
            } else {
                searches.put(current, "https://www.realitica.com/index.php?for=DuziNajam&lng=en&opa=" + city + "&cty=" + current);
            }
        }
        return searches;
    }

    /**
     * recursively collect urls for ad
     *
     * @param searches
     * @return
     */
    private List<String> extractFlatSearchUrlList(Map<String, Object> searches) {
        var result = new ArrayList<String>();
        for (var e : searches.entrySet()) {
            if (e.getValue() instanceof String) {
                result.add((String) e.getValue());
            } else if (e.getValue() instanceof Map) {
                var resultInternal = extractFlatSearchUrlList((Map) e.getValue());
                result.addAll(resultInternal);
            }
        }
        return result;
    }

    /**
     * Load ids of ad from search
     *
     * @param urlWithAds
     * @return
     */
    @SneakyThrows
    private HashSet<String> loadIdsBySearch(String urlWithAds) {
        var ids = new LinkedHashSet<String>();

        int curPage = 0;
        while (curPage >= 0) {
            try {
                var url = urlWithAds + "&cur_page=" + curPage;
                var pageDoc = Jsoup.connect(url).get();
                var adElements = pageDoc.select("div.thumb_div > a");
                if (adElements.size() == 0) {
                    log.info("Last page {} of {}", curPage + 1, urlWithAds);
                    curPage = -1;
                    continue;
                } else {
                    log.info("Loaded page {} of {}", curPage + 1, urlWithAds);
                    curPage++;
                }

                var listIds = adElements.stream()
                        .map(el -> el.attr("href"))
                        .filter(link -> link.startsWith("https://www.realitica.com/en/listing/"))
                        .map(link -> link.replace("https://www.realitica.com/en/listing/", ""))
                        .collect(Collectors.toList());
                ids.addAll(listIds);
            } catch (Throwable th) {
                log.error("Can't load page with ad, goes to sleep 1s: " + urlWithAds, th);
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
    public Map<String, String> loadAdAttributes(String id, int repeats) {
        if (repeats < 0) {
            return null;
        }

        try {
            log.info("Loading ad {}", id);

            var doc = Jsoup.connect(realiticaUrl + "/en/listing/" + id).get();
            var attributesMap = new LinkedHashMap<String, String>();

            var parentElements = doc.select("div");
            for (Element parentElement : parentElements) {
                var nodes = parentElement.childNodes();
                for (int i = 0; i < nodes.size(); i++) {
                    var node = nodes.get(i);
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
            log.error("Can't load ad {}", id, th);
            Thread.sleep(1000);
            return loadAdAttributes(id, repeats - 1);
        }
    }

    /**
     * load and save ad by id to db
     *
     * @param id
     * @param repeats
     */
    private void loadAdAndSave(String id, int repeats) {
        try {
            if (repeats < 0) {
                log.error("Will be not repeat for {}", id);
                return;
            }

            var adEntity = adRepository.findByRealiticaId(id);
            var attributesMap = loadAdAttributes(id, 1);
            if (attributesMap == null || attributesMap.isEmpty()) {
                if (adEntity != null) {
                    log.error("Attributes is empty for {}. Stun {} will be removed from DB", id, adEntity.getId());
                    adRepository.delete(adEntity);
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

            if (adEntity == null) {
                adEntity = new AdEntity();
                adEntity.setRealiticaId(id);
            }
            adEntity.setType(attributesMap.get("Type"));
            adEntity.setDistrict(attributesMap.get("District"));
            adEntity.setLocation(attributesMap.get("Location"));
            adEntity.setAddress(attributesMap.get("Address"));
            adEntity.setPrice(attributesMap.get("Price") != null ? attributesMap.get("Price").replace("€", "").replace(".", "") : null);
            adEntity.setBedrooms(attributesMap.get("Bedrooms"));
            adEntity.setLivingArea(attributesMap.get("Living Area"));
            adEntity.setMoreInfo(attributesMap.get("More info at"));
            adEntity.setLastModified(lastMobified);
            adEntity.setType(attributesMap.get("Type"));
            adEntity.setLink(realiticaUrl + "/en/listing/" + id);
            adRepository.save(adEntity);
            log.info("Save stun {}", id);
        } catch (Throwable th) {
            log.error("Can't save stun {}", id);
        }
    }
}
