package com.angrysurfer.core.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TableState {

    private List<String> columnOrder;

    // Default constructor uses default values
    public TableState() {
        this.columnOrder = new ArrayList<>();
    }

    // Add helper method
    public void setColumnOrderFromSet(Set<String> columns) {
        if (columns != null) {
            this.columnOrder = new ArrayList<>(columns);
        } else {
            this.columnOrder = new ArrayList<>();
        }
    }

    public Set<String> getColumnOrderAsSet() {
        if (columnOrder == null) {
            columnOrder = new ArrayList<>();
        }
        return new LinkedHashSet<>(columnOrder);
    }

    public void setColumnOrder(List<String> columnOrder) {
        if (columnOrder == null) {
            this.columnOrder = new ArrayList<>();
        } else {
            this.columnOrder = new ArrayList<>(columnOrder);
        }
    }

    public List<String> getColumnOrder() {
        if (columnOrder == null) {
            columnOrder = new ArrayList<>();
        }
        return new ArrayList<>(columnOrder); // Return a copy to prevent modification
    }
}
