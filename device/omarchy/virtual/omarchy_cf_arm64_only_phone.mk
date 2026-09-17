# Copyright 2026 OmarchyOS
# SPDX-License-Identifier: Apache-2.0

$(call inherit-product, device/google/cuttlefish/vsoc_arm64_only/phone/aosp_cf.mk)
$(call inherit-product, device/omarchy/virtual/omarchy_common.mk)

PRODUCT_NAME := omarchy_cf_arm64_only_phone
PRODUCT_MODEL := OmarchyOS Cuttlefish Phone
