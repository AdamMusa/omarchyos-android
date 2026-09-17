# Copyright 2026 OmarchyOS
# SPDX-License-Identifier: Apache-2.0

PRODUCT_MAKEFILES := \
    omarchy_cf_arm64_only_phone:$(LOCAL_DIR)/omarchy_cf_arm64_only_phone.mk \
    omarchy_sdk_phone64_arm64:$(LOCAL_DIR)/omarchy_sdk_phone64_arm64.mk

COMMON_LUNCH_CHOICES := \
    omarchy_cf_arm64_only_phone-trunk_staging-userdebug \
    omarchy_sdk_phone64_arm64-trunk_staging-userdebug
