package io.sited.page.service.message;

import io.sited.message.MessageHandler;
import io.sited.page.api.comment.CommentCreatedMessage;
import io.sited.page.service.PageStatisticsService;

import javax.inject.Inject;

/**
 * @author chi
 */
public class CommentCreatedMessageHandler implements MessageHandler<CommentCreatedMessage> {
    @Inject
    PageStatisticsService pageService;

    @Override
    public void handle(CommentCreatedMessage message) throws Throwable {
        pageService.commentCreated(message.pageId, 1, "SYS");
    }
}
