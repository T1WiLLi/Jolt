package io.github.t1willi;

import java.util.ArrayList;
import java.util.List;

import io.github.t1willi.injector.annotation.JoltBean;

@JoltBean
public class MyService {

    private List<String> list;

    public MyService() {
        this.list = new ArrayList<>(List.of("item1", "item2", "item3"));
    }

    public void addItem(String item) {
        list.add(item);
    }

    public List<String> getList() {
        return list;
    }
}
