package com.chatapp.api.controller;

import com.chatapp.api.model.MessageAttachment;
import com.chatapp.api.model.User;
import com.chatapp.api.repository.MessageAttachmentRepository;
import com.chatapp.api.repository.MessageRepository;
import com.chatapp.api.repository.RoomMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain", "application/zip",
        "application/x-rar-compressed", "application/x-7z-compressed",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public UploadController(RoomMemberRepository roomMemberRepository,
                             MessageRepository messageRepository,
                             MessageAttachmentRepository messageAttachmentRepository) {
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roomId") UUID roomId,
            @RequestParam(required = false) UUID messageId,
            @AuthenticationPrincipal User user) throws IOException {

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type " + contentType + " is not allowed");
        }

        if (file.getSize() > 52428800) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 50MB limit");
        }

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room");
        }

        if (messageId != null) {
            messageRepository.findByIdAndRoomId(messageId, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found in this room"));
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        UUID fileUuid = UUID.randomUUID();
        String storedName = fileUuid + ext;
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path destDir = Paths.get("uploads", datePath);
        Files.createDirectories(destDir);
        Path filePath = destDir.resolve(storedName);
        Files.write(filePath, file.getBytes());

        String fileUrl = "/uploads/" + datePath + "/" + storedName;
        String fileType = ALLOWED_CONTENT_TYPES.contains(contentType) ? "image" : "other";

        if (messageId != null) {
            MessageAttachment attachment = new MessageAttachment();
            attachment.setId(fileUuid);
            attachment.setMessageId(messageId);
            attachment.setFileName(originalFilename != null ? originalFilename : "file");
            attachment.setFileUrl(fileUrl);
            attachment.setFileType(fileType);
            attachment.setFileSize(file.getSize());
            messageAttachmentRepository.save(attachment);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", attachment.getId().toString(),
                "messageId", messageId.toString(),
                "fileName", attachment.getFileName(),
                "fileUrl", attachment.getFileUrl(),
                "fileType", attachment.getFileType(),
                "fileSize", attachment.getFileSize(),
                "mimeType", contentType
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", fileUuid.toString(),
            "fileName", originalFilename,
            "fileUrl", fileUrl,
            "fileType", fileType,
            "fileSize", file.getSize(),
            "mimeType", contentType
        ));
    }
}
