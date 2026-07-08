package it.mbc.hft.common.rem.model;

import it.mbc.hft.common.rem.constants.RemConstants;

import java.math.BigDecimal;

public record RtNativeBollingerProfile(
        String profileHash,
        int bbPeriod,
        BigDecimal bbStddevMultiplier,
        BigDecimal rangeMinDiscountToMiddlePct,
        BigDecimal rangeMinVolumeRatio,
        String rangeDmiMode,
        BigDecimal breakoutMinPremiumToMiddlePct,
        BigDecimal breakoutMaxPremiumToMiddlePct,
        BigDecimal breakoutMinVolumeRatio,
        BigDecimal breakoutMinRsi,
        BigDecimal lossCapAbs,
        BigDecimal breakoutMinProtectNetReturn,
        BigDecimal rangeMaxHoldCandles,
        BigDecimal breakoutMaxHoldCandles
) {
    public static RtNativeBollingerProfile f4e954() {
        return new RtNativeBollingerProfile(
                RemConstants.RT_NATIVE_F4E954_PROFILE_HASH,
                RemConstants.RT_NATIVE_F4E954_BB_PERIOD,
                RemConstants.RT_NATIVE_F4E954_BB_STDDEV_MULTIPLIER,
                RemConstants.RT_NATIVE_F4E954_RANGE_MIN_DISCOUNT_TO_MIDDLE_PCT,
                RemConstants.RT_NATIVE_F4E954_RANGE_MIN_VOLUME_RATIO,
                RemConstants.RT_NATIVE_F4E954_RANGE_DMI_MODE,
                RemConstants.RT_NATIVE_F4E954_BREAKOUT_MIN_PREMIUM_TO_MIDDLE_PCT,
                RemConstants.RT_NATIVE_F4E954_BREAKOUT_MAX_PREMIUM_TO_MIDDLE_PCT,
                RemConstants.RT_NATIVE_F4E954_BREAKOUT_MIN_VOLUME_RATIO,
                RemConstants.RT_NATIVE_F4E954_BREAKOUT_MIN_RSI,
                RemConstants.RT_NATIVE_F4E954_LOSS_CAP_ABS,
                RemConstants.RT_NATIVE_F4E954_BREAKOUT_MIN_PROTECT_NET_RETURN,
                RemConstants.RT_NATIVE_F4E954_RANGE_MAX_HOLD_CANDLES,
                RemConstants.RT_NATIVE_F4E954_BREAKOUT_MAX_HOLD_CANDLES
        );
    }
}
