// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

#![deny(missing_docs)]

//! Glean is a modern approach for recording and sending Telemetry data.
//!
//! It's in use at Mozilla.
//!
//! All documentation can be found online:
//!
//! ## [The Glean SDK Book](https://mozilla.github.io/glean)
//!
//! ## Example
//!
//! Initialize Glean, register a ping and then send it.
//!
//! ```rust,no_run
//! # use glean_preview::{Glean, Configuration, Error, metrics::*};
//! # fn main() -> Result<(), Error> {
//! let cfg = Configuration {
//!     data_path: "/tmp/data".into(),
//!     application_id: "org.mozilla.glean_core.example".into(),
//!     upload_enabled: true,
//!     max_events: None,
//! };
//! Glean.initialize(cfg)?;
//!
//! let prototype_ping = PingType::new("prototype", true, true);
//!
//! Glean.register_ping_type(&prototype_ping);
//!
//! prototype_ping.send();
//! # Ok(())
//! # }
//! ```

pub use glean_core::{Configuration, Error, Result};

pub mod metrics;

/// The object holding meta information about a Glean instance.
///
/// See `glean_core::Glean`.
pub struct Glean;

impl Glean {
    /// Create and initialize a new Glean object.
    ///
    /// See `glean_core::Glean::new`.
    pub fn initialize(self, cfg: Configuration) -> Result<()> {
        let glean = glean_core::Glean::new(cfg)?;
        glean_core::setup_glean(glean)
    }

    /// Set whether upload is enabled or not.
    ///
    /// See `glean_core::Glean.set_upload_enabled`.
    pub fn set_upload_enabled(self, flag: bool) -> bool {
        let mut glean = glean_core::global_glean().lock().unwrap();
        glean.set_upload_enabled(flag)
    }

    /// Determine whether upload is enabled.
    ///
    /// See `glean_core::Glean.set_upload_enabled`.
    #[allow(clippy::wrong_self_convention)]
    pub fn is_upload_enabled(self) -> bool {
        let glean = glean_core::global_glean().lock().unwrap();
        glean.is_upload_enabled()
    }

    /// Register a new `PingType`.
    ///
    /// See `glean_core::Glean.register_ping_type`.
    pub fn register_ping_type(self, ping: &metrics::PingType) {
        let mut glean = glean_core::global_glean().lock().unwrap();
        glean.register_ping_type(&ping.0)
    }

    /// Send a ping.
    ///
    /// See `glean_core::Glean.send_ping`.
    pub fn send_ping(self, ping: &metrics::PingType) -> Result<bool> {
        let glean = glean_core::global_glean().lock().unwrap();
        glean.send_ping(&ping.0)
    }

    /// Send a ping by name.
    ///
    /// See `glean_core::Glean.send_ping_by_name`.
    pub fn send_ping_by_name(self, ping: &str) -> Result<bool> {
        let glean = glean_core::global_glean().lock().unwrap();
        glean.send_ping_by_name(ping)
    }
}
