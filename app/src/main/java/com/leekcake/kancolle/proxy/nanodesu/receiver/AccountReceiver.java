package com.leekcake.kancolle.proxy.nanodesu.receiver;

import com.leekcake.kancolle.proxy.nanodesu.container.Account;

public interface AccountReceiver {
    void onAccountDetected(Account account);
    void onAccountChanged(Account account);
    void onAccountCleared();
}
