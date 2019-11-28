use std::env;

use once_cell::sync::Lazy;
use tempfile::Builder;

use glean_preview::{metrics::PingType, Configuration, Error, Glean};

#[allow(non_upper_case_globals)]
pub static PrototypePing: Lazy<PingType> = Lazy::new(|| PingType::new("prototype", true, true));

fn main() -> Result<(), Error> {
    env_logger::init();

    let mut args = env::args().skip(1);

    let data_path = if let Some(path) = args.next() {
        path
    } else {
        let root = Builder::new().prefix("simple-db").tempdir().unwrap();
        root.path().display().to_string()
    };

    let cfg = Configuration {
        data_path,
        application_id: "org.mozilla.glean_core.example".into(),
        upload_enabled: true,
        max_events: None,
    };
    Glean.initialize(cfg)?;
    Glean.register_ping_type(&PrototypePing);

    if let true = Glean.send_ping_by_name("prototype")? {
        log::info!("Successfully collected a prototype ping");
    } else {
        log::info!("Prototype ping failed");
    }

    Ok(())
}
