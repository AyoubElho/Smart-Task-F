package com.example.smarttask_frontend.entity;

public class CategoryDTO {

    private Long id;
    private String name;
    private String colorCode;

    public CategoryDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    @Override
    public String toString() {
        return name; // 🔥 IMPORTANT for ComboBox
    }
}
