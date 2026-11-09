pub mod cosmetic;
pub mod matcher;

use cosmetic::CosmeticEngine;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use matcher::{AdBlockEngine, ResourceType};
use once_cell::sync::Lazy;
use std::sync::RwLock;

static ADBLOCK_ENGINE: Lazy<RwLock<AdBlockEngine>> = Lazy::new(|| RwLock::new(AdBlockEngine::new()));
static COSMETIC_ENGINE: Lazy<RwLock<CosmeticEngine>> = Lazy::new(|| RwLock::new(CosmeticEngine::new()));

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeInit(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    // Engine initializes lazily on first access
    let _ = ADBLOCK_ENGINE.read();
    let _ = COSMETIC_ENGINE.read();
    JNI_TRUE
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeShouldBlock(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    origin: JString,
    resource_type: JString,
) -> jboolean {
    let url_str: String = match env.get_string(&url) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };

    let origin_str: String = match env.get_string(&origin) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let type_str: String = match env.get_string(&resource_type) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let res_type = ResourceType::from_str(&type_str);

    if let Ok(engine) = ADBLOCK_ENGINE.read() {
        if engine.should_block(&url_str, &origin_str, res_type) {
            return JNI_TRUE;
        }
    }

    JNI_FALSE
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeAddRule(
    mut env: JNIEnv,
    _class: JClass,
    rule: JString,
) -> jboolean {
    let rule_str: String = match env.get_string(&rule) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };

    let mut added = false;

    if rule_str.contains("##") {
        if let Ok(mut cosmetic) = COSMETIC_ENGINE.write() {
            added = cosmetic.parse_and_add_cosmetic(&rule_str);
        }
    } else {
        if let Ok(mut adblock) = ADBLOCK_ENGINE.write() {
            added = adblock.parse_and_add_rule(&rule_str);
        }
    }

    if added {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeLoadFilterRules(
    mut env: JNIEnv,
    _class: JClass,
    rules_text: JString,
) -> jint {
    let text: String = match env.get_string(&rules_text) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    let mut count = 0;
    let mut adblock_opt = ADBLOCK_ENGINE.write().ok();
    let mut cosmetic_opt = COSMETIC_ENGINE.write().ok();

    for line in text.lines() {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('!') || trimmed.starts_with('[') {
            continue;
        }

        if trimmed.contains("##") {
            if let Some(ref mut cosmetic) = cosmetic_opt {
                if cosmetic.parse_and_add_cosmetic(trimmed) {
                    count += 1;
                }
            }
        } else {
            if let Some(ref mut adblock) = adblock_opt {
                if adblock.parse_and_add_rule(trimmed) {
                    count += 1;
                }
            }
        }
    }

    count as jint
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeGetCosmeticCss(
    mut env: JNIEnv,
    _class: JClass,
    domain: JString,
) -> JString {
    let domain_str: String = match env.get_string(&domain) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let css = if let Ok(cosmetic) = COSMETIC_ENGINE.read() {
        cosmetic.generate_css_for_domain(&domain_str)
    } else {
        String::new()
    };

    match env.new_string(css) {
        Ok(js) => js,
        Err(_) => JString::default(),
    }
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeGetCosmeticScript(
    mut env: JNIEnv,
    _class: JClass,
    domain: JString,
) -> JString {
    let domain_str: String = match env.get_string(&domain) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    let script = if let Ok(cosmetic) = COSMETIC_ENGINE.read() {
        cosmetic.generate_injection_script(&domain_str)
    } else {
        String::new()
    };

    match env.new_string(script) {
        Ok(js) => js,
        Err(_) => JString::default(),
    }
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeGetBlockedCount(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    if let Ok(engine) = ADBLOCK_ENGINE.read() {
        engine.get_blocked_count() as jint
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_org_helium_tv_browser_adblock_RustAdBlocker_nativeResetBlockedCount(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Ok(engine) = ADBLOCK_ENGINE.read() {
        engine.reset_counter();
    }
}
