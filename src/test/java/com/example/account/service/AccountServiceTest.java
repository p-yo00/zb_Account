package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.account.type.ErrorCode.USER_NOT_FOUND;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account.builder()
                .accountNumber("1000000001")
                .accountUser(user)
                .build();

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(account));

        given(accountRepository.save(any())).willReturn(account);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto dto = accountService.createAccount(1L, 10000L);
        verify(accountRepository, times(1)).save(captor.capture());

        Assertions.assertEquals(captor.getValue().getAccountNumber(),
                "1000000002");
        Assertions.assertEquals(dto.getUserId(), 12L);
    }

    @Test
    void createFirstAccount() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any())).willReturn(Account.builder()
                .accountNumber("1000000001")
                .accountUser(user)
                .build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto dto = accountService.createAccount(1L, 10000L);

        verify(accountRepository, times(1)).save(captor.capture());
        Assertions.assertEquals(captor.getValue().getAccountNumber(),
                "1000000000");
        Assertions.assertEquals(dto.getUserId(), 12L);
    }

    @Test
    @DisplayName("계좌 생성 실패 - 해당 유저 없음")
    void createAccount_UserNotFound() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception =
                Assertions.assertThrows(AccountException.class,
                        () -> accountService.createAccount(1L, 10000L));

        // then
        Assertions.assertEquals(USER_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 최대 10개")
    void createAccount_MaxAccountIs10() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(AccountUser.builder()
                        .id(12L)
                        .name("Pororo")
                        .build()));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception =
                Assertions.assertThrows(AccountException.class,
                        () -> accountService.createAccount(1L, 10000L));

        // then
        Assertions.assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void deleteAccountSuccess() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000001")
                        .accountUser(user)
                        .balance(0L)
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto dto = accountService.deleteAccount(1L, "1000000001");
        verify(accountRepository, times(1)).save(captor.capture());

        Assertions.assertEquals(captor.getValue().getAccountStatus(),
                AccountStatus.UNREGISTERED);
    }

    @Test
    @DisplayName("계좌 해지 실패 - 해당 유저 없음")
    void deleteAccountFailed_UserNotFound() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception =
                Assertions.assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1000000000"));

        // then
        Assertions.assertEquals(USER_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 해당 계좌 없음")
    void deleteAccountFailed_AccountNotFound() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(AccountUser.builder()
                        .id(12L)
                        .name("Pororo")
                        .build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = Assertions.assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000001"));

        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌 소유주 불일치")
    void deleteAccountFailed_UserUnMatch() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(AccountUser.builder()
                        .id(10L)
                        .name("Pororo")
                        .build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000001")
                        .accountUser(AccountUser.builder()
                                .id(11L)
                                .name("Lupi")
                                .build())
                        .balance(0L)
                        .build()));

        // when
        AccountException exception = Assertions.assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000001"));
        // then
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.USER_ACCOUNT_UN_MATCH);
    }

    @Test
    @DisplayName("계좌 해지 실패 - 잔액이 남아있음")
    void deleteAccountFailed_BalanceNotEmpty() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000001")
                        .accountUser(user)
                        .balance(100L)
                        .build()));

        // when
        AccountException exception = Assertions.assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000001"));
        // then
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.ACCOUNT_NOT_EMPTY);
    }

    @Test
    @DisplayName("계좌 해지 실패 - 이미 해지된 계좌")
    void deleteAccountFailed_AlreadyUnregistered() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000001")
                        .accountUser(user)
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .build()));

        // when
        AccountException exception = Assertions.assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000001"));
        // then
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
    }

    @Test
    void successGetAccountsByUserId() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        List<Account> accounts =
                Arrays.asList(Account.builder()
                                .accountNumber("1234567890")
                                .balance(100L)
                                .accountUser(user)
                                .build(),
                        Account.builder()
                                .accountNumber("1211167890")
                                .accountUser(user)
                                .balance(200L).build()
                );
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        // when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        // then
        Assertions.assertEquals(2, accountDtos.size());
        Assertions.assertEquals(accountDtos.get(0).getAccountNumber(),
                accounts.get(0).getAccountNumber());
        Assertions.assertEquals(accountDtos.get(0).getBalance(),
                accounts.get(0).getBalance());
        Assertions.assertEquals(accountDtos.get(1).getAccountNumber(),
                accounts.get(1).getAccountNumber());
    }

    @Test
    void failedGetAccount() {
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        AccountException exception =
                Assertions.assertThrows(AccountException.class,
                        ()->accountService.getAccountsByUserId(1L));

        Assertions.assertEquals(exception.getErrorCode(), USER_NOT_FOUND);
    }
}