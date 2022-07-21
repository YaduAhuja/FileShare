package com.yaindustries.fileshare.exceptions;

public class PermissionNotFoundException extends Exception {
    public PermissionNotFoundException() {
        super("Permission Not Found");
    }
}
