import React, { useState, useEffect, useRef } from 'react';
import { Mic, MicOff, Send, Keyboard, MessageSquareOff } from 'lucide-react';
import { runAgentTurn } from '../services/agent';

export default function VoiceChat({ onStateChange }) {
  const [isListening, setIsListening] = useState(false);
  const [status, setStatus] = useState('idle'); // 'idle', 'listening', 'thinking', 'speaking'
  const [messages, setMessages] = useState([]);
  const [inputText, setInputText] = useState('');
  const [showKeyboard, setShowKeyboard] = useState(false);
  const [isVoiceModeActive, setIsVoiceModeActive] = useState(false);
  
  const recognitionRef = useRef(null);
  const synthRef = useRef(window.speechSynthesis);
  const textEndRef = useRef(null);
  
  // Refs to prevent stale closures in Speech Recognition callbacks
  const isVoiceModeActiveRef = useRef(isVoiceModeActive);
  const statusRef = useRef(status);
  
  useEffect(() => {
    isVoiceModeActiveRef.current = isVoiceModeActive;
  }, [isVoiceModeActive]);
  
  useEffect(() => {
    statusRef.current = status;
  }, [status]);
  
  useEffect(() => {
    // 1. Fetch chat history
    const history = JSON.parse(localStorage.getItem('chat_history') || '[]');
    setMessages(history);
    
    // 2. Initialize Speech Recognition
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      const rec = new SpeechRecognition();
      rec.continuous = true;
      rec.interimResults = true;
      rec.lang = 'en-US';
      
      let silenceTimer = null;
      
      rec.onstart = () => {
        setIsListening(true);
        setStatus('listening');
      };
      
      rec.onend = () => {
        setIsListening(false);
        if (statusRef.current === 'listening') {
          setStatus('idle');
        }
        if (silenceTimer) clearTimeout(silenceTimer);
      };
      
      rec.onresult = (event) => {
        if (statusRef.current !== 'listening') {
          return;
        }
        let finalTrans = '';
        let interimTrans = '';
        for (let i = 0; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            finalTrans += event.results[i][0].transcript + ' ';
          } else {
            interimTrans += event.results[i][0].transcript;
          }
        }
        
        const currentText = (finalTrans + interimTrans).trim();
        setInputText(currentText);
        setShowKeyboard(true); // Automatically show input text field to show transcription
        
        // Trigger send after 2.2 seconds of silence
        if (silenceTimer) clearTimeout(silenceTimer);
        silenceTimer = setTimeout(() => {
          if (currentText) {
            rec.stop();
            handleSendMsg(currentText);
          }
        }, 2200);
      };
      
      rec.onerror = (e) => {
        console.error('Speech recognition error:', e.error);
        setIsListening(false);
        setStatus('idle');
        if (silenceTimer) clearTimeout(silenceTimer);
      };
      
      recognitionRef.current = rec;
    }
    
    // Voices changed handler for speech synthesis loading
    const handleVoicesChanged = () => {
      if (window.speechSynthesis) {
        window.speechSynthesis.getVoices();
      }
    };
    
    if (window.speechSynthesis) {
      window.speechSynthesis.addEventListener('voiceschanged', handleVoicesChanged);
    }
    
    return () => {
      // NOTE: DO NOT cancel speech on status re-render! Only cancel on unmount (switching tabs).
      if (window.speechSynthesis) {
        window.speechSynthesis.cancel();
        window.speechSynthesis.removeEventListener('voiceschanged', handleVoicesChanged);
      }
    };
  }, []);

  useEffect(() => {
    // Scroll transcript to bottom on new messages
    textEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Speak advice using system TTS
  const speakText = (text) => {
    if (!window.speechSynthesis) {
      console.warn('Speech synthesis not supported');
      return;
    }
    
    try {
      // Un-pause and cancel any pending stuck speech queue
      window.speechSynthesis.resume();
      window.speechSynthesis.cancel();
      
      const utterance = new SpeechSynthesisUtterance(text);
      
      // Get stored voice parameters
      const rate = parseFloat(localStorage.getItem('voice_rate') || '1.0');
      const pitch = parseFloat(localStorage.getItem('voice_pitch') || '1.0');
      utterance.rate = rate;
      utterance.pitch = pitch;
      
      // Search for available male voices, but only assign if they are locally ready
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
      
      utterance.onstart = () => {
        setStatus('speaking');
      };
      
      utterance.onend = () => {
        setStatus('idle');
        // If voice conversation mode is active, auto-resume listening!
        if (isVoiceModeActive && recognitionRef.current) {
          try {
            recognitionRef.current.start();
          } catch (e) {
            // ignore
          }
        }
      };
      
      utterance.onerror = (e) => {
        console.error('Speech synthesis error callback:', e);
        setStatus('idle');
      };
      
      window.speechSynthesis.speak(utterance);
      
      // Force resume immediately to fix Google Chrome Android queue freezes
      window.speechSynthesis.resume();
    } catch (err) {
      console.error('Speech synthesis exception:', err);
      setStatus('idle');
    }
  };

  const handleSendMsg = async (text) => {
    if (!text.trim()) return;
    
    // Cancel speech ONLY if advisor is currently actively speaking
    if (synthRef.current && status === 'speaking') {
      synthRef.current.cancel();
    }
    
    setStatus('thinking');
    setInputText('');
    
    // Optimistically update messages list
    const currentHist = JSON.parse(localStorage.getItem('chat_history') || '[]');
    setMessages([...currentHist, { sender: 'user', message: text }]);
    
    // Call agent
    const reply = await runAgentTurn(text, () => {
      onStateChange();
    });
    
    // Fetch updated history
    const updatedHistory = JSON.parse(localStorage.getItem('chat_history') || '[]');
    setMessages(updatedHistory);
    
    // Narrate response
    speakText(reply);
  };

  const toggleMic = () => {
    if (!recognitionRef.current) {
      alert('Speech Recognition is not supported or permitted in this browser.');
      return;
    }
    
    // Un-mute/Unlock mobile Speech Synthesis on user gesture
    if (synthRef.current) {
      try {
        const silentUtterance = new SpeechSynthesisUtterance(" ");
        silentUtterance.volume = 0;
        synthRef.current.speak(silentUtterance);
      } catch (e) {
        console.error(e);
      }
    }
    
    if (isVoiceModeActive) {
      setIsVoiceModeActive(false);
      recognitionRef.current.stop();
      setIsListening(false);
      setStatus('idle');
    } else {
      setIsVoiceModeActive(true);
      if (synthRef.current) {
        synthRef.current.cancel();
      }
      try {
        recognitionRef.current.start();
      } catch (e) {
        console.error(e);
      }
    }
  };

  const handleClearHistory = () => {
    if (confirm('Clear conversation history?')) {
      localStorage.setItem('chat_history', JSON.stringify([]));
      setMessages([]);
      onStateChange();
    }
  };

  return (
    <div className="glass-panel" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minHeight: '65vh' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 className="section-title">Sleep Coach voice session</h2>
          <p className="section-subtitle">Speak or type your daily observations. I am listening.</p>
        </div>
        
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button 
            className={`icon-btn ${showKeyboard ? 'active-mic' : ''}`} 
            style={{ width: '38px', height: '38px', color: showKeyboard ? '#050716' : 'var(--text-secondary)' }}
            onClick={() => setShowKeyboard(!showKeyboard)}
            title="Toggle Typing Mode"
          >
            <Keyboard size={18} />
          </button>
          
          <button 
            className="icon-btn" 
            style={{ width: '38px', height: '38px', color: 'var(--text-secondary)' }}
            onClick={handleClearHistory}
            title="Clear Chat Logs"
          >
            <MessageSquareOff size={18} />
          </button>
        </div>
      </div>
      
      {/* Dynamic Pulsing Advisor Orb */}
      <div className="orb-container">
        <div className={`advisor-orb ${status}`} onClick={toggleMic}>
          {status === 'listening' && <Mic size={38} style={{ color: '#050716', animation: 'pulse 1.2s infinite' }} />}
          {status === 'thinking' && <Send size={38} style={{ color: 'white' }} />}
          {status === 'speaking' && <div style={{ fontSize: '1.5rem' }}>🔊</div>}
          {status === 'idle' && <MicOff size={38} style={{ color: '#8e9bb4' }} />}
        </div>
        
        {/* CSS Audio Waveform */}
        <div className={`waveform ${status === 'speaking' || status === 'listening' ? 'active' : ''}`}>
          <div className="wave-bar"></div>
          <div className="wave-bar"></div>
          <div className="wave-bar"></div>
          <div className="wave-bar"></div>
          <div className="wave-bar"></div>
          <div className="wave-bar"></div>
          <div className="wave-bar"></div>
        </div>
        
        <p style={{ marginTop: '1rem', fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 600 }}>
          {status === 'listening' && 'Listening... Speak now.'}
          {status === 'thinking' && 'Thinking... analyzing book guidelines...'}
          {status === 'speaking' && 'Speaking sleep advice...'}
          {status === 'idle' && 'Tap the orb to start voice check-in'}
        </p>
      </div>
      
      {/* Transcript Log */}
      <div className="transcript-box" style={{ flex: 1 }}>
        {messages.length === 0 ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem', padding: '2rem 0' }}>
            No conversation logs yet. Tap the microphone orb to report your sleep or ask a question.
          </p>
        ) : (
          messages.map((m, i) => (
            <div key={i} className={`transcript-msg ${m.sender}`} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem' }}>
              <span style={{ flex: 1 }}>{m.message}</span>
              {m.sender === 'advisor' && (
                <button 
                  onClick={() => speakText(m.message)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#b5a6ff', padding: '0.2rem', fontSize: '1rem', flexShrink: 0 }}
                  title="Speak Response"
                >
                  🔊
                </button>
              )}
            </div>
          ))
        )}
        <div ref={textEndRef} />
      </div>
      
      {/* Keyboard Input Fallback */}
      {showKeyboard && (
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
          <input
            type="text"
            className="form-input"
            style={{ flex: 1 }}
            placeholder="Type your message here..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSendMsg(inputText)}
          />
          <button className="primary-btn" onClick={() => handleSendMsg(inputText)}>
            <Send size={18} />
          </button>
        </div>
      )}
    </div>
  );
}
