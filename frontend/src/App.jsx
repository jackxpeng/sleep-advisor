import React, { useState, useEffect } from 'react';
import { 
  Calendar, Mic, PlusCircle, Wind, Brain, Settings as SettingsIcon, AlertCircle, Key
} from 'lucide-react';
import { initializeStorage } from './services/agent';

// Import subcomponents
import Dashboard from './components/Dashboard';
import VoiceChat from './components/VoiceChat';
import SleepDiaryForm from './components/SleepDiaryForm';
import RelaxationRoom from './components/RelaxationRoom';
import MemoryInspector from './components/MemoryInspector';
import Settings from './components/Settings';

import './App.css';

export default function App() {
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [hasApiKey, setHasApiKey] = useState(false);
  const [profileVersion, setProfileVersion] = useState(0); // revision trigger to force re-render

  useEffect(() => {
    // 1. Initialize local storage schema
    initializeStorage();
    
    // 2. Check for API key
    checkApiKey();
  }, [profileVersion]);

  const checkApiKey = () => {
    const key = localStorage.getItem('deepseek_api_key');
    setHasApiKey(!!key && key.trim().startsWith('sk-'));
  };

  const handleUpdate = () => {
    // Increment version trigger to force sub-components to reload storage
    setProfileVersion(prev => prev + 1);
    checkApiKey();
  };

  const getActiveWeekText = () => {
    const week = localStorage.getItem('cbt_week') || '-1';
    if (week === '-1') return 'Initial Interview';
    if (week === '0') return 'Baseline Logging';
    return `Week ${week} Focus`;
  };

  return (
    <div className="app-container">
      {/* Premium Top Navigation / Header */}
      <header className="app-header">
        <div className="brand-section">
          <img src="/sleep_logo.png" alt="Logo" className="brand-logo" />
          <h1 className="app-title">Sleep Advisor</h1>
        </div>
        <span className="cbt-badge">{getActiveWeekText()}</span>
      </header>

      {/* Main Workspace Frame */}
      <main className="app-main">
        
        {/* Warning notification if API Key is missing */}
        {!hasApiKey && currentTab !== 'settings' && (
          <div className="glass-panel" style={{ display: 'flex', gap: '1rem', alignItems: 'center', background: 'rgba(239, 71, 111, 0.08)', borderColor: 'rgba(239, 71, 111, 0.25)', marginBottom: '1.5rem' }}>
            <AlertCircle size={24} style={{ color: 'var(--red)', flexShrink: 0 }} />
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>DeepSeek API Key Required</div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                Please enter your DeepSeek API key in the settings to enable the AI Sleep Advisor.
              </div>
            </div>
            <button className="primary-btn" style={{ padding: '0.5rem 1rem', fontSize: '0.8rem' }} onClick={() => setCurrentTab('settings')}>
              <Key size={14} />
              Set Key
            </button>
          </div>
        )}

        {/* Tab Render Dispatcher */}
        {currentTab === 'dashboard' && (
          <Dashboard 
            profile={profileVersion} 
            onNavigate={(tab) => setCurrentTab(tab)} 
          />
        )}
        
        {currentTab === 'chat' && (
          <VoiceChat onStateChange={handleUpdate} />
        )}
        
        {currentTab === 'diary' && (
          <SleepDiaryForm onUpdate={handleUpdate} />
        )}
        
        {currentTab === 'relaxation' && (
          <RelaxationRoom onUpdate={handleUpdate} />
        )}
        
        {currentTab === 'memory' && (
          <MemoryInspector onUpdate={handleUpdate} />
        )}
        
        {currentTab === 'settings' && (
          <Settings onUpdate={handleUpdate} />
        )}
      </main>

      {/* Sticky Bottom Navigation Bar for Mobile Convenience */}
      <nav className="bottom-nav">
        <button 
          className={`nav-item ${currentTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setCurrentTab('dashboard')}
        >
          <Calendar />
          <span>Dashboard</span>
        </button>

        <button 
          className={`nav-item ${currentTab === 'chat' ? 'active' : ''}`}
          onClick={() => setCurrentTab('chat')}
        >
          <div style={{ position: 'relative' }}>
            <Mic />
            {/* Glowing gold dot if active voice check-in is loaded */}
            {hasApiKey && <span style={{ position: 'absolute', top: '-2px', right: '-2px', width: '6px', height: '6px', backgroundColor: 'var(--gold)', borderRadius: '50%', boxShadow: '0 0 6px var(--gold)' }}></span>}
          </div>
          <span>Advisor</span>
        </button>

        <button 
          className={`nav-item ${currentTab === 'diary' ? 'active' : ''}`}
          onClick={() => setCurrentTab('diary')}
        >
          <PlusCircle />
          <span>Diary</span>
        </button>

        <button 
          className={`nav-item ${currentTab === 'relaxation' ? 'active' : ''}`}
          onClick={() => setCurrentTab('relaxation')}
        >
          <Wind />
          <span>Breathe</span>
        </button>

        <button 
          className={`nav-item ${currentTab === 'memory' ? 'active' : ''}`}
          onClick={() => setCurrentTab('memory')}
        >
          <Brain />
          <span>Memory</span>
        </button>

        <button 
          className={`nav-item ${currentTab === 'settings' ? 'active' : ''}`}
          onClick={() => setCurrentTab('settings')}
        >
          <SettingsIcon />
          <span>Settings</span>
        </button>
      </nav>
    </div>
  );
}
