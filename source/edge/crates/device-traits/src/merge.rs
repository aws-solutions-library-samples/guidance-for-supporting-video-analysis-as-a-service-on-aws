// Code from https://github.com/jmfiaschi/json_value_merge/blob/v1.1.2/src/lib.rs
// The dependency is unstable and a change from pass by value to pass by ref was failing the build.
// This can be replaced with the updated version after the dependency is updated.

use serde_json::Value;

/// Trait used to merge Json Values
pub trait Merge {
    /// Method use to merge two Json Values : ValueA <- ValueB.
    fn merge(&mut self, new_json_value: &Value);
}

impl Merge for Value {
    fn merge(&mut self, new_json_value: &Value) {
        merge(self, new_json_value);
    }
}

fn merge(a: &mut Value, b: &Value) {
    match (a, b) {
        (Value::Object(ref mut a), Value::Object(ref b)) => {
            for (k, v) in b {
                merge(a.entry(k).or_insert(Value::Null), v);
            }
        }
        (Value::Array(ref mut a), Value::Array(ref b)) => {
            a.extend(b.clone());
        }
        (Value::Array(ref mut a), Value::Object(ref b)) => {
            a.extend([Value::Object(b.clone())]);
        }
        (a, b) => {
            *a = b.clone();
        }
    }
}