package de.unijena.bioinf.ms.middleware;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.function.Consumer;
import java.util.function.Function;

public class Pages {

    public static <T> void forEach(int pageSize, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageConsumer){
        forEach(PageRequest.ofSize(pageSize), pageProvider, pageConsumer);
    }

    public static  <T> void forEach(int pageNumberFrom, int pageSize, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageConsumer){
        forEach(PageRequest.of(pageNumberFrom, pageSize), pageProvider, pageConsumer);
    }

    public static  <T> void forEach(@NotNull Pageable startPage, Function<Pageable, Page<T>> pageProvider, Consumer<Page<T>> pageProcessor){
        Pageable pageable = startPage;
        Page<T> page;
        do {
            page = pageProvider.apply(pageable); // Fetch the current page
            pageProcessor.accept(page); // Your custom method to process the page

            pageable = page.hasNext() ? page.nextPageable() : Pageable.unpaged(); // Move to next page

        } while (page.hasNext());
    }
}
