use regex::Regex;
use std::collections::{HashMap, HashSet};
use std::sync::atomic::{AtomicUsize, Ordering};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ResourceType {
    Script,
    Image,
    Subdocument,
    Stylesheet,
    Media,
    Font,
    XmlHttpRequest,
    WebSocket,
    Other,
}

impl ResourceType {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "script" => ResourceType::Script,
            "image" | "img" => ResourceType::Image,
            "subdocument" | "iframe" => ResourceType::Subdocument,
            "stylesheet" | "css" => ResourceType::Stylesheet,
            "media" | "video" | "audio" => ResourceType::Media,
            "font" => ResourceType::Font,
            "xmlhttprequest" | "xhr" | "fetch" => ResourceType::XmlHttpRequest,
            "websocket" => ResourceType::WebSocket,
            _ => ResourceType::Other,
        }
    }
}

#[derive(Debug, Clone)]
pub struct FilterRule {
    pub raw: String,
    pub is_exception: bool,
    pub domain_anchor: Option<String>,
    pub url_pattern: Option<Regex>,
    pub raw_substring: Option<String>,
    pub domains_restricted: HashSet<String>,
    pub domains_excluded: HashSet<String>,
}

pub struct AdBlockEngine {
    blocking_rules: Vec<FilterRule>,
    exception_rules: Vec<FilterRule>,
    domain_blacklist: HashSet<String>,
    blocked_counter: AtomicUsize,
}

impl AdBlockEngine {
    pub fn new() -> Self {
        let mut engine = Self {
            blocking_rules: Vec::with_capacity(4096),
            exception_rules: Vec::with_capacity(256),
            domain_blacklist: HashSet::with_capacity(2048),
            blocked_counter: AtomicUsize::new(0),
        };
        engine.load_default_popular_rules();
        engine
    }

    fn load_default_popular_rules(&mut self) {
        // High-frequency known tracker and ad network hosts
        let default_domains = [
            "doubleclick.net",
            "googleadservices.com",
            "googlesyndication.com",
            "adnxs.com",
            "criteo.com",
            "adroll.com",
            "outbrain.com",
            "taboola.com",
            "scorecardresearch.com",
            "quantserve.com",
            "advertising.com",
            "popcash.net",
            "popads.net",
            "adsterra.com",
            "propellerads.com",
            "exoclick.com",
            "trafficjunky.com",
            "yandex.ru/ads",
            "moatads.com",
            "hotjar.com",
            "clarity.ms",
        ];

        for domain in default_domains {
            self.domain_blacklist.insert(domain.to_string());
        }

        // Add standard blocking rules
        self.parse_and_add_rule("||doubleclick.net^");
        self.parse_and_add_rule("||googleadservices.com^");
        self.parse_and_add_rule("||googlesyndication.com^");
        self.parse_and_add_rule("||adnxs.com^");
        self.parse_and_add_rule("||taboola.com^");
        self.parse_and_add_rule("||outbrain.com^");
        self.parse_and_add_rule("/adserver/*");
        self.parse_and_add_rule("/ads/*");
        self.parse_and_add_rule("/popunder/*");
        self.parse_and_add_rule("/banner/*");
    }

    pub fn parse_and_add_rule(&mut self, rule_line: &str) -> bool {
        let trimmed = rule_line.trim();
        if trimmed.is_empty() || trimmed.starts_with('!') || trimmed.starts_with('[') {
            return false;
        }

        // Check if cosmetic filter (handled in cosmetic module)
        if trimmed.contains("##") || trimmed.contains("#@#") || trimmed.contains("#?#") {
            return false;
        }

        let is_exception = trimmed.starts_with("@@");
        let rule_content = if is_exception {
            &trimmed[2..]
        } else {
            trimmed
        };

        let mut parts = rule_content.splitn(2, '$');
        let pattern_part = parts.next().unwrap_or("").trim();
        let _options_part = parts.next();

        let mut domain_anchor = None;
        let mut raw_substring = None;
        let mut regex_pattern = None;

        if pattern_part.starts_with("||") {
            let without_anchor = &pattern_part[2..];
            let clean_domain = without_anchor.trim_matches('^');
            domain_anchor = Some(clean_domain.to_lowercase());
        } else if pattern_part.starts_with('/') && pattern_part.ends_with('/') && pattern_part.len() > 2 {
            let re_str = &pattern_part[1..pattern_part.len() - 1];
            if let Ok(re) = Regex::new(re_str) {
                regex_pattern = Some(re);
            }
        } else if pattern_part.contains('*') {
            let escaped = regex::escape(pattern_part).replace("\\*", ".*");
            if let Ok(re) = Regex::new(&escaped) {
                regex_pattern = Some(re);
            }
        } else {
            raw_substring = Some(pattern_part.to_lowercase());
        }

        let filter = FilterRule {
            raw: trimmed.to_string(),
            is_exception,
            domain_anchor,
            url_pattern: regex_pattern,
            raw_substring,
            domains_restricted: HashSet::new(),
            domains_excluded: HashSet::new(),
        };

        if is_exception {
            self.exception_rules.push(filter);
        } else {
            if let Some(ref d) = filter.domain_anchor {
                self.domain_blacklist.insert(d.clone());
            }
            self.blocking_rules.push(filter);
        }
        true
    }

    pub fn should_block(&self, request_url: &str, _page_origin: &str, _resource_type: ResourceType) -> bool {
        let url_lower = request_url.to_lowercase();

        // Check exception whitelist first
        for exc in &self.exception_rules {
            if self.matches_rule(exc, &url_lower) {
                return false;
            }
        }

        // Fast domain check
        for black_domain in &self.domain_blacklist {
            if url_lower.contains(black_domain) {
                self.blocked_counter.fetch_add(1, Ordering::Relaxed);
                return true;
            }
        }

        // Deep matching against blocking rules
        for rule in &self.blocking_rules {
            if self.matches_rule(rule, &url_lower) {
                self.blocked_counter.fetch_add(1, Ordering::Relaxed);
                return true;
            }
        }

        false
    }

    fn matches_rule(&self, rule: &FilterRule, url_lower: &str) -> bool {
        if let Some(ref anchor) = rule.domain_anchor {
            if !url_lower.contains(anchor) {
                return false;
            }
        }

        if let Some(ref sub) = rule.raw_substring {
            if !url_lower.contains(sub) {
                return false;
            }
        }

        if let Some(ref re) = rule.url_pattern {
            if !re.is_match(url_lower) {
                return false;
            }
        }

        true
    }

    pub fn get_blocked_count(&self) -> usize {
        self.blocked_counter.load(Ordering::Relaxed)
    }

    pub fn reset_counter(&self) {
        self.blocked_counter.store(0, Ordering::Relaxed);
    }
}
