// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

use crate::Result;

/// Stores information about a ping.
///
/// This is required so that given metric data queued on disk we can send
/// pings with the correct settings, e.g. whether it has a client_id.
#[derive(Clone, Debug)]
pub struct PingType(pub(crate) glean_core::metrics::PingType);

impl PingType {
    /// Create a new ping type for the given name and whether to include the client ID when
    /// sending.
    ///
    /// ## Arguments
    ///
    /// * `name` - The name of the ping.
    /// * `include_client_id` - Whether to include the client ID in the assembled ping when.
    /// sending.
    pub fn new<A: Into<String>>(name: A, include_client_id: bool, send_if_empty: bool) -> Self {
        Self(glean_core::metrics::PingType {
            name: name.into(),
            include_client_id,
            send_if_empty,
        })
    }

    /// Send the ping.
    ///
    /// ## Arguments
    ///
    /// * `glean` - the Glean instance to use to send the ping.
    ///
    /// ## Return value
    ///
    /// See [`Glean#send_ping`](../struct.Glean.html#method.send_ping) for details.
    pub fn send(&self) -> Result<bool> {
        let glean = glean_core::global_glean().lock().unwrap();
        glean.send_ping(&self.0)
    }
}
