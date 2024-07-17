package org.openedx.core.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme colors scheme
val light_primary = Color(0xFF00262B)   // Primary 500
val light_primary_variant = Color(0xFF002121) // Primary 700
val light_secondary = Color(0xFF1F453D) // Brand 500
val light_secondary_variant = Color(0xFF707070) // Gray 500
val light_background = Color.White
val light_surface = Color(0xFFFBFAF9) // Off-White
val light_error = Color(0xFFAB0D02) // Danger 500
val light_warning = Color(0xFFF0CC00) // Warning 300
val light_info = Color(0xFF00688D)  // Info 500
val light_info_variant = Color(0xFF1C8DBE)  // Info 300

val light_onPrimary = Color.White
val light_onSecondary = Color.White
val light_onBackground = light_primary
val light_onSurface = Color(0xFF454545) // Gray 700
val light_onError = Color.White
val light_onWarning = Color.White
val light_onInfo = Color.White

val light_success_green = Color(0xFF0D7D4D)// Color(0xFFF2FAF7) // Success 500
val light_success_background = Color(0xFFF2FAF7)//Color(0xFF0D7D4D) // Success 100

val light_text_primary = light_primary   // Primary 500
val light_text_primary_variant = Color(0xFF454545)  // Gray 700
val light_text_primary_light = light_secondary_variant
val light_text_hyper_link = light_info // Info 500

val light_text_secondary = light_primary  // Primary 500 | Dark 500 | Elm
val light_text_dark = light_primary_variant // Primary 700 | Dark 700
val light_text_warning = light_primary_variant // Primary 700 | Dark 700

val light_text_accent = Color(0xFF03C7E8) // Accent A Isotope Blue

val light_text_field_background = light_surface
val light_text_field_background_variant = light_surface
val light_text_field_border = light_onSurface
val light_text_field_text = light_text_primary
val light_text_field_hint = light_secondary_variant

val light_primary_button_background = Color(0xFFD74000)   // Brand 500
val light_primary_button_text = light_surface
val light_primary_button_border = light_primary
val light_primary_button_bordered_text = light_primary

val light_secondary_button_background = light_primary
val light_secondary_button_text = light_background
val light_secondary_button_border = light_primary_button_background
val light_secondary_button_bordered_background = light_surface
val light_secondary_button_bordered_text = light_primary_button_background

val light_card_view_background = light_surface
val light_card_view_border = light_text_field_border

val light_divider = light_primary
val light_certificate_foreground = light_surface
val light_bottom_sheet_toggle = light_text_accent

val light_rate_stars = light_warning
val light_inactive_button_background = Color(0xFFFCFCFC)
val light_access_green = Color(0xFF23BCA0)
val light_dates_section_bar_past_due = Color(0xFFFFC248)
val light_dates_section_bar_today = Color(0xFF5DE3BF)
val light_dates_section_bar_this_week = light_secondary
val light_dates_section_bar_next_week = Color(0xFF798F8B)
val light_dates_section_bar_upcoming = Color(0xFFA5B5B1)
val light_auth_sso_success_background = light_success_green
val light_auth_google_button_background = Color.White
val light_auth_facebook_button_background = Color(0xFF0866FF)
val light_auth_microsoft_button_background = Color(0xFF2E2E2E)
val light_component_horizontal_progress_completed_and_selected = light_primary
val light_component_horizontal_progress_completed = Color(0xFF8F8F8F) // Gray 400
val light_component_horizontal_progress_selected = light_primary
val light_component_horizontal_progress_default = Color(0xFF8F8F8F) // Gray 400

val light_tab_unselected_btn_background = light_background
val light_tab_unselected_btn_content = light_primary
val light_tab_selected_btn_content = light_background
val light_course_home_header_shade = Color(0xFFBABABA)
val light_course_home_back_btn_background = light_surface
val light_settings_title_content = light_surface
val light_progress_bar_color = light_primary_button_background
val light_progress_bar_background_color = light_secondary_variant


// Dark theme colors scheme
val dark_primary = Color(0xFFFBFAF9) // Light 200
val dark_primary_variant = Color(0xFFF2F0EF) // Light 300
val dark_secondary = Color(0xFFD23228)  // Brand 500
val dark_secondary_variant = Color(0xFFD23228)  // Brand 500
val dark_background = Color(0xFF00262b)  // Primary 500 | Dark 500
val dark_surface = Color(0xFF1F453D) // Primary 500 | Dark 700
val dark_error = Color(0xFFAB0D02)   // Danger 500
val dark_warning = Color(0xFFF0CC00) // Accent B Oxide Yellow
val dark_info = Color(0xFF03C7E8)  // Accent A Isotope Blue
val dark_info_variant = Color(0xFF00688D)  // Info 500

val dark_onPrimary = Color(0xFF002121)   // Primary 700 | Dark 700
val dark_onSecondary = Color.White
val dark_onBackground = dark_primary
val dark_onSurface = Color.White
val dark_onError = Color.White
val dark_onWarning = Color.White
val dark_onInfo = Color.White

val dark_success_green = Color(0xFF0D7D4D) // success 500
val dark_success_background = Color.White

val dark_text_primary = dark_primary
val dark_text_primary_variant = Color(0xFFF2F0EF) // Light 300
val dark_text_primary_light = Color(0xFF707070) // Gray 500
val dark_text_hyper_link = Color(0xFF00688D) // Info 500

val dark_text_secondary = Color.White
val dark_text_dark = dark_text_primary
val dark_text_warning = Color(0xFF002121) // primary 700

val dark_text_accent = Color(0xFF03C7E8) // Accent A Isotope Blue
val dark_text_field_background = dark_surface
val dark_text_field_background_variant = dark_surface
val dark_text_field_border = Color(0xFFD2DAD8) // Gray 500
val dark_text_field_text = dark_text_primary
val dark_text_field_hint = Color(0xFFD2DAD8) // Gray 700

val dark_primary_button_background = Color(0xFFD74000)   // Primary 500 | Dark 500 | Elm
val dark_primary_button_text = Color.White
val dark_primary_button_border = dark_primary
val dark_primary_button_bordered_text = dark_primary

val dark_secondary_button_background = dark_primary
val dark_secondary_button_text = dark_background
val dark_secondary_button_border = Color(0xFFD7D3D1) // Light 700
val dark_secondary_button_bordered_background = Color.White
val dark_secondary_button_bordered_text = Color(0xFFD23228) // Brand 500

val dark_card_view_background = dark_surface
val dark_card_view_border = Color(0xFF4E5A70)
val dark_divider = dark_primary

val dark_certificate_foreground = Color(0xD92EB865)
val dark_bottom_sheet_toggle = Color(0xFF03C7E8) // Accent A Isotope Blue
val dark_rate_stars = Color(0xFFF0CC00) // Accent B Oxide Yellow
val dark_inactive_button_background = Color(0xFFCCD4E0)
val dark_access_green = Color(0xFF23BCA0)
val dark_dates_section_bar_past_due = Color(0xFFFFC248)
val dark_dates_section_bar_today = Color(0xFF5DE3BF)
val dark_dates_section_bar_this_week = Color(0xFFA5B5B1)
val dark_dates_section_bar_next_week = Color(0xFF798F8B)
val dark_dates_section_bar_upcoming = Color(0xFF1F453D)
val dark_auth_sso_success_background = dark_success_green
val dark_auth_google_button_background = Color.White
val dark_auth_facebook_button_background = Color(0xFF0866FF)
val dark_auth_microsoft_button_background = Color(0xFF2E2E2E)
val dark_component_horizontal_progress_completed_and_selected = Color.White
val dark_component_horizontal_progress_completed = Color(0xFF8F8F8F)
val dark_component_horizontal_progress_selected = Color.White
val dark_component_horizontal_progress_default = Color(0xFF8F8F8F)
val dark_tab_unselected_btn_background = dark_background
val dark_tab_unselected_btn_content = dark_text_primary
val dark_tab_selected_btn_content = dark_background
val dark_course_home_header_shade = Color(0xFF999999)
val dark_course_home_back_btn_background = Color.Black
val dark_settings_title_content = Color.White
val dark_progress_bar_color = dark_primary_button_background
val dark_progress_bar_background_color = Color(0xFF8E9BAE)
