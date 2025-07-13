package io.github.t1willi.pipeline;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class EncodingStep implements PipelineStep {

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        return true;
    }
}
