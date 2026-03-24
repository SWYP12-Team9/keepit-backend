package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkStreamPayloadMetadataMissingException extends BusinessException {

    public LinkStreamPayloadMetadataMissingException() {
        super(ErrorCode.LINK_STREAM_PAYLOAD_METADATA_MISSING);
    }
}
