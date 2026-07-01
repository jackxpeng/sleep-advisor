import React, { useState, useEffect, useRef } from 'react';
import { Wind, Play, Pause, RefreshCw, Volume2, VolumeX, CheckCircle } from 'lucide-react';

export default function RelaxationRoom({ onUpdate }) {
  const [isRunning, setIsRunning] = useState(false);
  const [breathPhase, setBreathPhase] = useState('rest'); // 'rest', 'inhale', 'hold', 'exhale', 'hold_empty'
  const [secondsLeft, setSecondsLeft] = useState(4); // counts down each step
  const [totalTimer, setTotalTimer] = useState(600); // 10 minutes default (in seconds)
  const [durationPreset, setDurationPreset] = useState(600);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [technique, setTechnique] = useState('4-4-4-4'); // '4-4-4-4' (Box), '4-7-8' (Calming)
  const [exerciseCompleted, setExerciseCompleted] = useState(false);

  const timerIntervalRef = useRef(null);
  const phaseTimeoutRef = useRef(null);
  const totalTimerIntervalRef = useRef(null);

  useEffect(() => {
    return () => {
      stopExercise();
    };
  }, []);

  const playChime = (freq, duration = 0.6) => {
    if (!soundEnabled) return;
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return;
      const ctx = new AudioContext();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, ctx.currentTime);
      
      gain.gain.setValueAtTime(0.06, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + duration);
      
      osc.connect(gain);
      gain.connect(ctx.destination);
      
      osc.start();
      osc.stop(ctx.currentTime + duration);
    } catch (e) {
      console.error(e);
    }
  };

  const startExercise = () => {
    setIsRunning(true);
    setExerciseCompleted(false);
    playChime(523.25, 0.8); // C5 chime to start
    runPhase('inhale');
    
    // Start countdown for overall timer
    totalTimerIntervalRef.current = setInterval(() => {
      setTotalTimer(prev => {
        if (prev <= 1) {
          clearInterval(totalTimerIntervalRef.current);
          completeExercise();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const stopExercise = () => {
    setIsRunning(false);
    setBreathPhase('rest');
    setSecondsLeft(4);
    
    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    if (totalTimerIntervalRef.current) clearInterval(totalTimerIntervalRef.current);
    if (phaseTimeoutRef.current) clearTimeout(phaseTimeoutRef.current);
  };

  const completeExercise = () => {
    stopExercise();
    setExerciseCompleted(true);
    playChime(659.25, 1.2); // E5 soothing final chime
    
    // Log in local storage for today's checklist
    const todayStr = new Date().toISOString().split('T')[0];
    localStorage.setItem(`relaxation_done_${todayStr}`, 'true');
    onUpdate();
  };

  const runPhase = (phase) => {
    setBreathPhase(phase);
    
    let duration = 4;
    let nextPhase = 'hold';
    let chimeFreq = 329.63; // E4

    if (technique === '4-4-4-4') {
      // Box Breathing
      if (phase === 'inhale') {
        duration = 4;
        nextPhase = 'hold';
        chimeFreq = 329.63; // E4
      } else if (phase === 'hold') {
        duration = 4;
        nextPhase = 'exhale';
        chimeFreq = 392.00; // G4
      } else if (phase === 'exhale') {
        duration = 4;
        nextPhase = 'hold_empty';
        chimeFreq = 261.63; // C4
      } else if (phase === 'hold_empty') {
        duration = 4;
        nextPhase = 'inhale';
        chimeFreq = 293.66; // D4
      }
    } else {
      // 4-7-8 Breathing
      if (phase === 'inhale') {
        duration = 4;
        nextPhase = 'hold';
        chimeFreq = 329.63; // E4
      } else if (phase === 'hold') {
        duration = 7;
        nextPhase = 'exhale';
        chimeFreq = 392.00; // G4
      } else if (phase === 'exhale') {
        duration = 8;
        nextPhase = 'inhale';
        chimeFreq = 261.63; // C4
      }
    }

    setSecondsLeft(duration);
    playChime(chimeFreq, 0.4);

    // Set countdown interval for active phase
    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    timerIntervalRef.current = setInterval(() => {
      setSecondsLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerIntervalRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    // Timeout to trigger next phase
    if (phaseTimeoutRef.current) clearTimeout(phaseTimeoutRef.current);
    phaseTimeoutRef.current = setTimeout(() => {
      runPhase(nextPhase);
    }, duration * 1000);
  };

  const handleReset = () => {
    stopExercise();
    setTotalTimer(durationPreset);
    setExerciseCompleted(false);
  };

  const changePreset = (seconds) => {
    stopExercise();
    setDurationPreset(seconds);
    setTotalTimer(seconds);
    setExerciseCompleted(false);
  };

  const formatTotalTime = (sec) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const getPromptText = () => {
    if (!isRunning) return 'Ready';
    if (breathPhase === 'inhale') return 'BREATHE IN';
    if (breathPhase === 'hold') return 'HOLD';
    if (breathPhase === 'exhale') return 'BREATHE OUT';
    if (breathPhase === 'hold_empty') return 'HOLD';
    return 'Ready';
  };

  const getSubPromptText = () => {
    if (!isRunning) return 'Tap start to begin';
    if (breathPhase === 'inhale') return 'Expand your abdomen slowly';
    if (breathPhase === 'hold') return 'Rest in the fullness';
    if (breathPhase === 'exhale') return 'Release all air and tension';
    if (breathPhase === 'hold_empty') return 'Rest in the emptiness';
    return '';
  };

  return (
    <div className="glass-panel">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 className="section-title">Relaxation Room</h2>
          <p className="section-subtitle">Consciously elicit the Relaxation Response to slow brain waves.</p>
        </div>
        <button 
          className="icon-btn" 
          style={{ width: '38px', height: '38px', color: 'var(--text-secondary)' }}
          onClick={() => setSoundEnabled(!soundEnabled)}
          title={soundEnabled ? 'Mute Chimes' : 'Enable Chimes'}
        >
          {soundEnabled ? <Volume2 size={18} /> : <VolumeX size={18} />}
        </button>
      </div>

      <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', margin: '1rem 0' }}>
        <div style={{ flex: 1, minWidth: '140px' }} className="form-group">
          <label className="form-label">Breathing Pattern</label>
          <select 
            className="form-select" 
            value={technique} 
            onChange={(e) => { stopExercise(); setTechnique(e.target.value); }}
            disabled={isRunning}
          >
            <option value="4-4-4-4">Box Breathing (4-4-4-4)</option>
            <option value="4-7-8">Calming Breath (4-7-8)</option>
          </select>
        </div>
        
        <div style={{ flex: 1, minWidth: '140px' }} className="form-group">
          <label className="form-label">Duration Preset</label>
          <div style={{ display: 'flex', gap: '0.25rem' }}>
            <button className={`primary-btn secondary ${durationPreset === 120 ? 'active' : ''}`} style={{ padding: '0.5rem', fontSize: '0.8rem' }} onClick={() => changePreset(120)} disabled={isRunning}>2m</button>
            <button className={`primary-btn secondary ${durationPreset === 300 ? 'active' : ''}`} style={{ padding: '0.5rem', fontSize: '0.8rem' }} onClick={() => changePreset(300)} disabled={isRunning}>5m</button>
            <button className={`primary-btn secondary ${durationPreset === 600 ? 'active' : ''}`} style={{ padding: '0.5rem', fontSize: '0.8rem' }} onClick={() => changePreset(600)} disabled={isRunning}>10m</button>
            <button className={`primary-btn secondary ${durationPreset === 900 ? 'active' : ''}`} style={{ padding: '0.5rem', fontSize: '0.8rem' }} onClick={() => changePreset(900)} disabled={isRunning}>15m</button>
          </div>
        </div>
      </div>

      {/* Main Breathing Pacer Interface */}
      <div className="breathing-pacer-container">
        <div style={{ fontSize: '2rem', fontFamily: 'var(--font-title)', fontWeight: 800, color: 'var(--gold)' }}>
          {formatTotalTime(totalTimer)}
        </div>

        <div className="outer-breathing-ring">
          <div 
            className={`breathing-circle ${isRunning ? breathPhase : 'rest'}`}
          >
            <div style={{ fontSize: '1.8rem', color: '#050716', fontWeight: '800' }}>
              {isRunning ? secondsLeft : <Wind size={32} />}
            </div>
          </div>
        </div>

        <div style={{ textAlign: 'center', minHeight: '60px' }}>
          <div className="breathing-prompt">
            {getPromptText()}
          </div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.4rem' }}>
            {getSubPromptText()}
          </div>
        </div>
      </div>

      {/* Control Buttons */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem' }}>
        {isRunning ? (
          <button className="primary-btn" onClick={stopExercise} style={{ background: 'var(--red)' }}>
            <Pause size={18} />
            Pause
          </button>
        ) : (
          <button className="primary-btn" onClick={startExercise} style={{ background: 'var(--cyan)', color: '#050716' }}>
            <Play size={18} />
            Start Session
          </button>
        )}
        
        <button className="primary-btn secondary" onClick={handleReset}>
          <RefreshCw size={18} />
          Reset
        </button>
      </div>

      {exerciseCompleted && (
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.2)', padding: '1.25rem', borderRadius: '16px', marginTop: '1.5rem' }}>
          <CheckCircle size={24} style={{ color: 'var(--cyan)', flexShrink: 0 }} />
          <div style={{ fontSize: '0.85rem', color: '#c4f4e7', lineHeight: '1.4' }}>
            <strong>Session Completed!</strong> You successfully practiced the Relaxation Response. Abdominal breathing slows your sympathetic nervous system and is logged on today's sleep checklist. Keep it up!
          </div>
        </div>
      )}
    </div>
  );
}
