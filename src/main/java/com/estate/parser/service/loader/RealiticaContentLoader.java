package com.estate.parser.service.loader;

import com.estate.parser.entity.AdEntity;
import com.estate.parser.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.estate.parser.entity.AdEntity.Type.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealiticaContentLoader implements IContentLoader {

    private final AdRepository adRepository;

    @Value("${realitica.url:https://www.realitica.com}")
    private String baseUrl;

    @Override
    public List<String> loadAndSave() {
        var searchesByCitiesAndAreas = loadSearchesByCitiesAndAreas(baseUrl + "/rentals/Montenegro/", null);
        var searches = extractFlatSearchUrlList(searchesByCitiesAndAreas);
        return searches.stream()
                .map(this::loadIdsBySearch)
                .flatMap(Collection::stream)
                .map(id -> loadAdAndSave(id, 1))
                .filter(Objects::nonNull)
                .map(AdEntity::getSourceId)
                .toList();
    }

    @Override
    public String getSourceName() {
        return "realitica";
    }

    @Override
    public boolean isCanBeDeleted(String sourceId) {
        var attributesMap = loadAdAttributes(sourceId, 1);
        return attributesMap == null || attributesMap.isEmpty();
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
            searches.put("All-Rental", "https://www.realitica.com/index.php?for=DuziNajam&lng=en&opa=" + city);
            searches.put("All-Sale", "https://www.realitica.com/index.php?for=Prodaja&lng=en&opa=" + city);
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
                searches.put(current, "https://www.realitica.com/index.php?for=Prodaja&lng=en&opa=" + city + "&cty=" + current);
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
        log.info("Start to load by filter: {}", urlWithAds);
        var ids = new LinkedHashSet<String>();

        int curPage = 0;
        while (curPage >= 0) {
            try {
                var url = urlWithAds + "&cur_page=" + curPage;
                var pageDoc = Jsoup.connect(url).get();
                var adElements = pageDoc.select("div.thumb_div > a");
                if (adElements.isEmpty()) {
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
                        .toList();
                ids.addAll(listIds);
            } catch (Exception e) {
                log.error("Can't load page with ad, goes to sleep 1s: " + urlWithAds, e);
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
    private Map<String, String> loadAdAttributes(String id, int repeats) {
        if (repeats < 0) {
            return null;
        }

        try {
            log.info("Loading ad {}", id);

            var doc = Jsoup.connect(baseUrl + "/en/listing/" + id).get();
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
        } catch (Exception e) {
            log.error("Can't load ad {}", id, e);
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
    private AdEntity loadAdAndSave(String id, int repeats) {
        try {
            if (repeats < 0) {
                log.error("Will be not repeat for {}", id);
                return null;
            }

            var adEntity = adRepository.findBySourceIdAndSourceCode(id, "realitica");
            var attributesMap = loadAdAttributes(id, 1);
            if (attributesMap == null || attributesMap.isEmpty()) {
                log.error("Attributes is empty for {}. Stun will be skipped, not founded in DB", id);
                return null;
            }

            var lastModifiedStr = attributesMap.get("Last Modified");
            var lastModified = switch (lastModifiedStr) {
                case null -> null;
                default -> {
                    try {
                        yield LocalDate.parse(lastModifiedStr, DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH));
                    } catch (Exception e) {
                        log.error("Can't parse data {}, {}", id, attributesMap.get("Last Modified"), e);
                        yield null;
                    }
                }
            };


            if (adEntity == null) {
                adEntity = new AdEntity();
                adEntity.setSourceId(id);
                adEntity.setSourceCode("realitica");
                adEntity.setSourceLink(baseUrl + "/en/listing/" + id);
            }
            adEntity.setCity(attributesMap.get("District"));
            adEntity.setLocation(attributesMap.get("Location"));
            adEntity.setAddress(attributesMap.get("Address"));
            adEntity.setPrice(attributesMap.get("Price") != null ? attributesMap.get("Price").replace("€", "").replace(".", "") : null);
            adEntity.setBedrooms(attributesMap.get("Bedrooms"));
            adEntity.setSize(attributesMap.get("Living Area"));
            adEntity.setDetails(attributesMap.get("More info at"));
            adEntity.setLastModified(lastModified == null ? null : lastModified.atStartOfDay());
            adEntity.setType(convertType(attributesMap.get("Type")));
            adRepository.save(adEntity);
            log.info("Save stun {}", id);

            return adEntity;
        } catch (Exception e) {
            log.error("Can't save stun {}", id, e);
            return null;
        }
    }

    private AdEntity.Type convertType(String type) {
        return switch (type) {
            case "Apartment For Sale" -> APARTMENT_FOR_SALE;
            case "Apartment Long Term Rental", "Room Long Term Rental" -> APARTMENT_LONG_TERM_RENTAL;
            case "House For Sale" -> HOUSE_FOR_SALE;
            case "House Long Term Rental" -> HOUSE_LONG_TERM_RENTAL;
            case "Land For Sale", "Agricultural Land For Sale" -> LAND_FOR_SALE;
            case "Land Long Term Rental" -> LAND_LONG_TERM_RENTAL;
            case "Residential Lot For Sale" -> RESIDENTIAL_FOR_SALE;
            case "Residential Lot Long Term Rental" -> RESIDENTIAL_LONG_TERM_RENTAL;
            case "Commercial Property For Sale", "Hotel For Sale", "Campground For Sale" -> COMMERCIAL_FOR_SALE;
            case "Commercial Property Long Term Rental", "Hotel Long Term Rental" -> COMMERCIAL_LONG_TERM_RENTAL;
            case "Garage For Sale" -> GARAGE_FOR_SALE;
            case "Garage Long Term Rental" -> GARAGE_LONG_TERM_RENTAL;
            default -> OTHER;
        };
    }
}
