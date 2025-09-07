package org.example.todo.server.protocol;

public class ListTasksRequest {
    public String boardId;
    public Filters filters;
    public Sort sort;

    public static class Filters {
        public String[] status;   // e.g., ["todo","inProgress"]
        public String[] priority; // e.g., ["high","low"]
        public Long dueBefore;
        public Long dueAfter;
    }

    public static class Sort {
        public String by;    // createdAt | due | priority
        public String order; // asc | desc
    }
}
