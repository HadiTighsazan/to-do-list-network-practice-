package org.example.todo.server.protocol;

import java.util.List;

public class ViewBoardResponse {
    public BoardSummary board;
    public List<BoardMemberView> members;
}
