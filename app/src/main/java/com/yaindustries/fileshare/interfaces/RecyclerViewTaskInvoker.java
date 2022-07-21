package com.yaindustries.fileshare.interfaces;

@FunctionalInterface
public interface RecyclerViewTaskInvoker {
    void onClickTask(int position);

    default void onLongClickTask(int position) {

    }
}
