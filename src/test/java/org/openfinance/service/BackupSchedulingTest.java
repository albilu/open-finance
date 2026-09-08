package org.openfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfinance.entity.Backup;
import org.openfinance.entity.User;
import org.openfinance.repository.BackupRepository;
import org.openfinance.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackupSchedulingTest {
    @Mock private BackupRepository backups;
    @Mock private UserRepository users;
    @Mock private UserBackupArchive archives;
    @InjectMocks private BackupService service;
    @TempDir Path directory;
    private final List<Backup> records = new ArrayList<>();
    private long nextId;

    @BeforeEach
    void configureStorage() throws Exception {
        ReflectionTestUtils.setField(service, "databaseUrl", "jdbc:sqlite:fixture.db");
        ReflectionTestUtils.setField(service, "backupDirectory", directory.toString());
        ReflectionTestUtils.setField(service, "scheduleEnabled", true);
        ReflectionTestUtils.setField(service, "retentionCount", 1);
        when(users.findAll())
                .thenReturn(List.of(User.builder().id(1L).build(), User.builder().id(2L).build()));
        when(backups.save(any()))
                .thenAnswer(
                        call -> {
                            Backup backup = call.getArgument(0);
                            if (backup.getId() == null) {
                                backup.setId(++nextId);
                                records.add(backup);
                            }
                            return backup;
                        });
        when(backups.findByUserIdAndBackupTypeOrderByCreatedAtDesc(anyLong(), eq("AUTOMATIC")))
                .thenAnswer(
                        call ->
                                records.stream()
                                        .filter(
                                                backup ->
                                                        backup.getUserId()
                                                                .equals(call.getArgument(0)))
                                        .sorted(Comparator.comparing(Backup::getId).reversed())
                                        .toList());
        when(backups.findByIdAndUserId(anyLong(), anyLong()))
                .thenAnswer(
                        call ->
                                records.stream()
                                        .filter(
                                                backup ->
                                                        backup.getId().equals(call.getArgument(0))
                                                                && backup.getUserId()
                                                                        .equals(
                                                                                call.getArgument(
                                                                                        1)))
                                        .findFirst());
        doAnswer(call -> records.remove(call.getArgument(0))).when(backups).delete(any());
        doAnswer(call -> Files.writeString(call.getArgument(1), "User " + call.getArgument(0)))
                .when(archives)
                .write(anyLong(), any(Path.class));
    }

    @Test
    void scheduleCreatesDistinctFilesAndAppliesRetentionForEachUser() throws Exception {
        service.scheduledBackup();
        List<Path> firstFiles =
                records.stream().map(backup -> Path.of(backup.getFilePath())).toList();
        assertThat(firstFiles).hasSize(2).doesNotHaveDuplicates();
        service.scheduledBackup();
        assertThat(records).hasSize(2);
        assertThat(records).extracting(Backup::getUserId).containsExactlyInAnyOrder(1L, 2L);
        for (Backup backup : records) {
            assertThat(backup.getStatus()).isEqualTo("COMPLETED");
            assertThat(backup.getFileSize()).isPositive();
            assertThat(Files.exists(Path.of(backup.getFilePath()))).isTrue();
        }
        for (Path old : firstFiles) assertThat(Files.exists(old)).isFalse();
    }
}
