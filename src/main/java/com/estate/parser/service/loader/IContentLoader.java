package com.estate.parser.service.loader;

import java.util.List;

public interface IContentLoader {
    List<String> loadAndSave();

    String getSourceName();

    boolean isCanBeDeleted(String sourceId);
}
