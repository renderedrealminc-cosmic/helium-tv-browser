use std::collections::HashMap;

pub struct CosmeticEngine {
    generic_selectors: Vec<String>,
    domain_selectors: HashMap<String, Vec<String>>,
}

impl CosmeticEngine {
    pub fn new() -> Self {
        let mut engine = Self {
            generic_selectors: Vec::with_capacity(1024),
            domain_selectors: HashMap::with_capacity(256),
        };
        engine.load_default_cosmetics();
        engine
    }

    fn load_default_cosmetics(&mut self) {
        // High frequency generic ad containers
        let default_generic = [
            ".adsbygoogle",
            "div[id^='google_ads']",
            "div[class*='ad-banner']",
            "div[class*='sponsored-content']",
            "div[class*='taboola']",
            "div[class*='outbrain']",
            ".ad-container",
            ".advertisement",
            ".ad-slot",
            "aside[aria-label='advertisement']",
            "iframe[src*='doubleclick']",
            "iframe[id^='google_ads_frame']",
        ];

        for s in default_generic {
            self.generic_selectors.push(s.to_string());
        }
    }

    pub fn parse_and_add_cosmetic(&mut self, line: &str) -> bool {
        let trimmed = line.trim();
        if trimmed.is_empty() || trimmed.starts_with('!') {
            return false;
        }

        if let Some(pos) = trimmed.find("##") {
            let domains_part = &trimmed[..pos];
            let selector_part = &trimmed[pos + 2..];

            if selector_part.is_empty() {
                return false;
            }

            if domains_part.is_empty() {
                self.generic_selectors.push(selector_part.to_string());
            } else {
                for domain in domains_part.split(',') {
                    let d = domain.trim().to_lowercase();
                    self.domain_selectors
                        .entry(d)
                        .or_insert_with(Vec::new)
                        .push(selector_part.to_string());
                }
            }
            return true;
        }

        false
    }

    pub fn generate_css_for_domain(&self, domain: &str) -> String {
        let domain_clean = domain.to_lowercase();
        let mut selectors = self.generic_selectors.clone();

        if let Some(specific) = self.domain_selectors.get(&domain_clean) {
            selectors.extend(specific.clone());
        }

        if selectors.is_empty() {
            return String::new();
        }

        format!(
            "{} {{ display: none !important; visibility: hidden !important; height: 0 !important; width: 0 !important; }}",
            selectors.join(", ")
        )
    }

    pub fn generate_injection_script(&self, domain: &str) -> String {
        let css = self.generate_css_for_domain(domain);
        if css.is_empty() {
            return String::new();
        }
        format!(
            r#"(function() {{
                const style = document.createElement('style');
                style.setAttribute('data-helium-adblock', 'true');
                style.textContent = `{}`;
                (document.head || document.documentElement).appendChild(style);
            }})();"#,
            css.replace('`', "\\`")
        )
    }
}
