package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

        Account account = Account.builder()
                .accountNumber("1000000001")
                .accountUser(user)
                .build();

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());

        given(accountRepository.save(any())).willReturn(account);

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
        Assertions.assertEquals(ErrorCode.USER_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 최대 10개")
    void createAccount_MaxAccountIs10() {
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Pororo")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

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
}