package org.openfinance.testutil;

import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;
import org.openfinance.entity.LiabilityPrincipalAllocation;
import org.openfinance.repository.LiabilityPrincipalAllocationRepository;

/** A stateful repository fixture: reads observe writes and reversal deletes. */
public final class PrincipalAllocationRepositoryMocks {
    private PrincipalAllocationRepositoryMocks() {}

    public static LiabilityPrincipalAllocationRepository create() {
        LiabilityPrincipalAllocationRepository repository =
                Mockito.mock(LiabilityPrincipalAllocationRepository.class);
        List<LiabilityPrincipalAllocation> rows = new ArrayList<>();
        Mockito.lenient()
                .when(repository.save(Mockito.any()))
                .thenAnswer(
                        call -> {
                            LiabilityPrincipalAllocation row = call.getArgument(0);
                            rows.add(row);
                            return row;
                        });
        Mockito.lenient()
                .when(repository.findByTrancheIdAndUserId(Mockito.any(), Mockito.any()))
                .thenAnswer(
                        call ->
                                rows.stream()
                                        .filter(
                                                r ->
                                                        java.util.Objects.equals(
                                                                        r.getTrancheId(),
                                                                        call.getArgument(0))
                                                                && r.getUserId()
                                                                        .equals(
                                                                                call.getArgument(
                                                                                        1)))
                                        .toList());
        Mockito.lenient()
                .when(repository.findByTransactionIdAndUserId(Mockito.any(), Mockito.any()))
                .thenAnswer(
                        call ->
                                rows.stream()
                                        .filter(
                                                r ->
                                                        r.getTransactionId()
                                                                        .equals(call.getArgument(0))
                                                                && r.getUserId()
                                                                        .equals(
                                                                                call.getArgument(
                                                                                        1)))
                                        .toList());
        Mockito.lenient()
                .when(repository.findByLiabilityIdAndUserId(Mockito.any(), Mockito.any()))
                .thenAnswer(
                        call ->
                                rows.stream()
                                        .filter(
                                                r ->
                                                        r.getLiabilityId()
                                                                        .equals(call.getArgument(0))
                                                                && r.getUserId()
                                                                        .equals(
                                                                                call.getArgument(
                                                                                        1)))
                                        .toList());
        Mockito.lenient()
                .doAnswer(
                        call -> {
                            rows.removeIf(
                                    r ->
                                            r.getTransactionId().equals(call.getArgument(0))
                                                    && r.getUserId().equals(call.getArgument(1)));
                            return null;
                        })
                .when(repository)
                .deleteByTransactionIdAndUserId(Mockito.any(), Mockito.any());
        return repository;
    }
}
