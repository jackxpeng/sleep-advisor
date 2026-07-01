import React, { useState, useEffect } from 'react';
import { Key, RotateCcw, Database, Speech, Eye, EyeOff } from 'lucide-react';
import { initializeStorage } from '../services/agent';

export default function Settings({ onUpdate }) {
  const [apiKey, setApiKey] = useState('');
  const [showKey, setShowKey] = useState(false);
  const [voiceRate, setVoiceRate] = useState(1.0);
  const [voicePitch, setVoicePitch] = useState(0.9);
  
  useEffect(() => {
    setApiKey(localStorage.getItem('deepseek_api_key') || '');
    setVoiceRate(parseFloat(localStorage.getItem('voice_rate') || '1.0'));
    setVoicePitch(parseFloat(localStorage.getItem('voice_pitch') || '0.9'));
  }, []);

  const handleSaveKey = () => {
    localStorage.setItem('deepseek_api_key', apiKey.trim());
    alert('API Key updated successfully.');
    onUpdate();
  };

  const handleSaveVoiceSettings = () => {
    localStorage.setItem('voice_rate', voiceRate.toString());
    localStorage.setItem('voice_pitch', voicePitch.toString());
    alert('Voice settings updated.');
  };

  const handleTestVoice = () => {
    if (!window.speechSynthesis) {
      alert('Speech synthesis is not supported on this browser.');
      return;
    }
    
    try {
      // Un-pause and cancel any pending speech
      window.speechSynthesis.resume();
      window.speechSynthesis.cancel();
      
      const utterance = new SpeechSynthesisUtterance("Hello there. Can you hear my voice? I am your sleep advisor, ready to help you sleep.");
      utterance.rate = voiceRate;
      utterance.pitch = voicePitch;
      
      const voices = window.speechSynthesis.getVoices();
      let maleVoice = voices.find(v => 
        v.lang.startsWith('en') && 
        (v.name.toLowerCase().includes('male') || v.name.toLowerCase().includes('google-us') || v.name.toLowerCase().includes('google uk'))
      );
      if (!maleVoice) {
        maleVoice = voices.find(v => v.lang.startsWith('en-GB') || v.name.toLowerCase().includes('en-gb'));
      }
      
      if (maleVoice) {
        utterance.voice = maleVoice;
      }
      
      window.speechSynthesis.speak(utterance);
      window.speechSynthesis.resume();
    } catch (e) {
      alert('Test voice error: ' + e.message);
    }
  };

  const handleResetData = () => {
    if (confirm('Are you sure you want to delete all sleep diaries, memory profiles, and chat logs? This will reset the app to day one.')) {
      localStorage.clear();
      initializeStorage();
      setApiKey('');
      setVoiceRate(1.0);
      setVoicePitch(0.9);
      alert('All application data has been reset.');
      onUpdate();
    }
  };

  const handleLoadMockData = () => {
    if (confirm('This will insert 7 days of realistic baseline sleep diaries, showing minor insomnia (average sleep efficiency ~78%). This helps you test the CBT statistics and advance the program week immediately. Proceed?')) {
      initializeStorage();
      
      const mockDiaries = [
        {
          date: '2026-06-22',
          bed_time: '23:00',
          light_out_time: '23:15',
          latency_mins: 45,
          awakenings: 2,
          awake_mins: 40,
          wake_time: '06:30',
          out_of_bed_time: '06:45',
          quality: 2,
          alertness: 3,
          medications: 'None',
          notes: 'Had a hard time shutting my mind off. Woke up feeling tired.'
        },
        {
          date: '2026-06-23',
          bed_time: '23:00',
          light_out_time: '23:30',
          latency_mins: 50,
          awakenings: 3,
          awake_mins: 35,
          wake_time: '06:15',
          out_of_bed_time: '06:30',
          quality: 2,
          alertness: 2,
          medications: 'None',
          notes: 'Felt frustrated lying in bed. NST: I will never get to sleep.'
        },
        {
          date: '2026-06-24',
          bed_time: '22:45',
          light_out_time: '23:00',
          latency_mins: 30,
          awakenings: 1,
          awake_mins: 20,
          wake_time: '06:00',
          out_of_bed_time: '06:15',
          quality: 3,
          alertness: 4,
          medications: 'None',
          notes: 'Slightly better. Still woke up in the middle of the night.'
        },
        {
          date: '2026-06-25',
          bed_time: '23:30',
          light_out_time: '23:45',
          latency_mins: 60,
          awakenings: 2,
          awake_mins: 50,
          wake_time: '06:45',
          out_of_bed_time: '07:00',
          quality: 1,
          alertness: 2,
          medications: 'None',
          notes: 'Awful night. Stressed about work tomorrow.'
        },
        {
          date: '2026-06-26',
          bed_time: '23:00',
          light_out_time: '23:15',
          latency_mins: 40,
          awakenings: 2,
          awake_mins: 30,
          wake_time: '06:30',
          out_of_bed_time: '06:45',
          quality: 3,
          alertness: 3,
          medications: 'None',
          notes: 'Average sleep, felt a bit sleepy in afternoon.'
        },
        {
          date: '2026-06-27',
          bed_time: '22:30',
          light_out_time: '23:00',
          latency_mins: 55,
          awakenings: 3,
          awake_mins: 45,
          wake_time: '07:00',
          out_of_bed_time: '07:30',
          quality: 2,
          alertness: 2,
          medications: 'None',
          notes: 'Stayed in bed too long trying to sleep. Felt groggy.'
        },
        {
          date: '2026-06-28',
          bed_time: '23:00',
          light_out_time: '23:15',
          latency_mins: 35,
          awakenings: 2,
          awake_mins: 25,
          wake_time: '06:30',
          out_of_bed_time: '06:45',
          quality: 3,
          alertness: 3,
          medications: 'None',
          notes: 'Logging baseline complete. Sleep efficiency calculated around 79%.'
        }
      ];
      
      localStorage.setItem('sleep_diaries', JSON.stringify(mockDiaries));
      localStorage.setItem('cbt_week', '0'); // Move from interview to baseline logging completed state (or baseline week)
      
      // Update Core Memory Progress
      const human = JSON.parse(localStorage.getItem('core_memory_human') || '{}');
      human.cbt_progress = {
        current_week: 0,
        current_week_description: "Baseline Logging completed (Ready for Week 1)",
        sleep_window: "Not set",
        average_sleep_duration: 320, // 5 hours 20 mins
        average_sleep_efficiency: 78.5
      };
      localStorage.setItem('core_memory_human', JSON.stringify(human, null, 2));
      
      alert('Mock baseline data successfully generated!');
      onUpdate();
    }
  };

  return (
    <div className="glass-panel">
      <h2 className="section-title">Settings & Configuration</h2>
      <p className="section-subtitle">Manage your credentials, voice advisor preferences, and application storage.</p>
      
      <div className="settings-section-title">DeepSeek API Key</div>
      <div className="form-group">
        <label className="form-label">API Key</label>
        <div style={{ display: 'flex', gap: '0.5rem', position: 'relative' }}>
          <input
            type={showKey ? 'text' : 'password'}
            className="form-input"
            style={{ flex: 1, paddingRight: '3rem' }}
            placeholder="sk-..."
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
          />
          <button 
            type="button"
            className="icon-btn" 
            style={{ width: '40px', height: '40px', position: 'absolute', right: '10px', top: '4px', border: 'none', background: 'none' }}
            onClick={() => setShowKey(!showKey)}
          >
            {showKey ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
          The key is stored only on your Pixel 10 browser storage. We never save it to any external server.
        </p>
        <button className="primary-btn" style={{ marginTop: '0.75rem' }} onClick={handleSaveKey}>
          <Key size={18} />
          Save API Key
        </button>
      </div>

      <div className="settings-section-title">Voice Coach Settings</div>
      <div className="form-group">
        <label className="form-label">Voice Speech Speed (Rate): {voiceRate}x</label>
        <input
          type="range"
          min="0.7"
          max="1.3"
          step="0.05"
          value={voiceRate}
          onChange={(e) => setVoiceRate(parseFloat(e.target.value))}
          style={{ width: '100%', margin: '0.5rem 0' }}
        />
      </div>
      
      <div className="form-group">
        <label className="form-label">Voice Pitch (Deepness): {voicePitch}</label>
        <input
          type="range"
          min="0.5"
          max="1.2"
          step="0.05"
          value={voicePitch}
          onChange={(e) => setVoicePitch(parseFloat(e.target.value))}
          style={{ width: '100%', margin: '0.5rem 0' }}
        />
        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
          Lower pitch yields a deeper, more confident male tone. Default is 0.9.
        </p>
        <div style={{ display: 'flex', gap: '1rem', marginTop: '0.75rem' }}>
          <button className="primary-btn" onClick={handleSaveVoiceSettings}>
            <Speech size={18} />
            Save Voice Settings
          </button>
          <button className="primary-btn secondary" onClick={handleTestVoice}>
            🔊 Test Voice
          </button>
        </div>
      </div>

      <div className="settings-section-title">CBT-I Testing Utilities</div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', marginTop: '0.5rem' }}>
        <button className="primary-btn secondary" onClick={handleLoadMockData}>
          <Database size={18} />
          Insert 7-Day Baseline Logs
        </button>
        
        <button className="primary-btn secondary" style={{ color: 'var(--red)', borderColor: 'rgba(239, 71, 111, 0.3)' }} onClick={handleResetData}>
          <RotateCcw size={18} />
          Reset App Data
        </button>
      </div>
    </div>
  );
}
