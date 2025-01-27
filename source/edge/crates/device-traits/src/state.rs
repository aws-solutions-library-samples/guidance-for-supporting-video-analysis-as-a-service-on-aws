///Used for communication between threads to organize what resources should be active at any given time.
/// https://doc.rust-lang.org/std/sync/atomic/
/// Atomics prevent thread locking but are hardware defined.
use std::sync::atomic::{AtomicUsize, Ordering};
static GLOBAL_SYSTEM_FLAG: AtomicUsize =
    AtomicUsize::new(State::DisableStreamingResources as usize);

/// Enum to track the state of the system. Alerts streaming resources.
#[derive(Debug, Copy, Clone, Eq, PartialEq)]
pub enum State {
    ///Flag to create + enable cloud streaming resources.
    CreateOrEnableSteamingResources = 1,
    ///Flag to disable streaming resources.  Resources are not dropped.
    DisableStreamingResources = 2,
}

impl From<State> for usize {
    fn from(val: State) -> usize {
        val as usize
    }
}

impl From<usize> for State {
    fn from(value: usize) -> Self {
        match value {
            1 => State::CreateOrEnableSteamingResources,
            2 => State::DisableStreamingResources,
            _ => panic!("Invalid value passed in."),
        }
    }
}

/// This struct wraps static functions to set the state + get it everywhere in the system
/// It is built off of the atomic integer which does not block threads.
#[derive(Debug)]
pub struct StateManager {}

impl StateManager {
    /// Get the state of the device.  Used for determining the setting of resources in the system.
    pub fn get_state() -> State {
        GLOBAL_SYSTEM_FLAG.load(Ordering::Relaxed).into()
    }
    /// Set the state of the device.  Set by cloud operations.
    pub fn set_state(new_state: State) {
        GLOBAL_SYSTEM_FLAG.store(new_state.into(), Ordering::Relaxed);
    }
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn check_set_state() {
        // Since global data is used move all tests here to ensure ordering of tests.
        assert_eq!(StateManager::get_state(), State::DisableStreamingResources);

        StateManager::set_state(State::CreateOrEnableSteamingResources);
        assert_eq!(StateManager::get_state(), State::CreateOrEnableSteamingResources);
        StateManager::set_state(State::DisableStreamingResources);
        assert_eq!(StateManager::get_state(), State::DisableStreamingResources);
    }
}
