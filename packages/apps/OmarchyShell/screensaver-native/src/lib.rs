//! Android host for the unchanged upstream Omarchy ttfx animation engine.
//! Handles are confined to the Dream's rendering worker, never shared with Qt.
use clap::{CommandFactory, Parser};
use jni::{JNIEnv, objects::{JClass, JString}, sys::{jint, jlong, jstring}};
use std::{panic::{catch_unwind, AssertUnwindSafe}, ptr};
use ttfx::{cli::Cli, engine::{ctx::{Clock, EngineCtx}, effect::Effect}, utils::rng::Rng};

struct Animation {
    effect: Box<dyn Effect>,
    ctx: EngineCtx,
}

fn create(input: &str, width: i32, height: i32, seed: i64) -> Result<Animation, String> {
    if !(16..=120).contains(&width) || !(8..=160).contains(&height) || input.len() > 16384 {
        return Err("Invalid animation canvas".into());
    }
    let mut rng = Rng::seeded(seed as u64);
    let names: Vec<String> = Cli::command().get_subcommands()
        .map(|c| c.get_name().to_string()).filter(|n| n != "help").collect();
    let name = &names[rng.choice_index(names.len())];
    let args = ["ttfx", "--canvas-width", &width.to_string(), "--canvas-height", &height.to_string(),
        "--anchor-canvas", "c", "--anchor-text", "c", "--ignore-terminal-dimensions",
        "--frame-rate", "60", name];
    let cli = Cli::try_parse_from(args).map_err(|e| e.to_string())?;
    let config = cli.terminal_config();
    let mut ctx = EngineCtx::new(input, config, rng, Clock::virtual_with_frame_rate(60))
        .map_err(|e| format!("{e}"))?;
    let mut effect = cli.effect.ok_or("Missing effect")?.build_effect();
    effect.build(&mut ctx).map_err(|e| format!("{e}"))?;
    Ok(Animation { effect, ctx })
}

#[no_mangle]
pub extern "system" fn Java_os_omarchy_shell_OmarchyTextEffects_nativeCreate(
    mut env: JNIEnv, _: JClass, input: JString, width: jint, height: jint, seed: jlong,
) -> jlong {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let input: String = env.get_string(&input).map_err(|e| e.to_string())?.into();
        create(&input, width, height, seed)
    }));
    match result {
        Ok(Ok(animation)) => Box::into_raw(Box::new(animation)) as jlong,
        error => {
            let message = match error { Ok(Err(e)) => e, _ => "Animation initialization failed".into() };
            let _ = env.throw_new("java/lang/IllegalStateException", message);
            0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_os_omarchy_shell_OmarchyTextEffects_nativeNext(
    mut env: JNIEnv, _: JClass, handle: jlong,
) -> jstring {
    if handle == 0 { return ptr::null_mut(); }
    let result = catch_unwind(AssertUnwindSafe(|| {
        // Java confines creation, stepping, and destruction to one HandlerThread.
        let animation = unsafe { &mut *(handle as *mut Animation) };
        // 60 simulation steps / second, displayed at 30 fps for a phone power budget.
        let first = animation.effect.next_frame(&mut animation.ctx)?;
        Some(animation.effect.next_frame(&mut animation.ctx).unwrap_or(first))
    }));
    match result {
        Ok(Some(frame)) => env.new_string(frame).map(|s| s.into_raw()).unwrap_or(ptr::null_mut()),
        Ok(None) => ptr::null_mut(),
        Err(_) => {
            let _ = env.throw_new("java/lang/IllegalStateException", "Animation frame failed");
            ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_os_omarchy_shell_OmarchyTextEffects_nativeDestroy(
    _: JNIEnv, _: JClass, handle: jlong,
) {
    if handle != 0 { unsafe { drop(Box::from_raw(handle as *mut Animation)); } }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn upstream_effects_produce_changing_phone_frames() {
        for seed in 0..8 {
            let mut a = create("OMARCHY", 40, 24, seed).unwrap();
            let mut frames = std::collections::HashSet::new();
            for _ in 0..120 {
                if let Some(f) = a.effect.next_frame(&mut a.ctx) { frames.insert(f); }
                else { break; }
            }
            assert!(frames.len() > 1, "seed {seed} did not animate");
        }
    }

    #[test]
    fn reject_unbounded_canvases() {
        assert!(create("OMARCHY", 10000, 10000, 1).is_err());
    }
}
