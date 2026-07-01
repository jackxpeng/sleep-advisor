import React, { useState, useEffect } from 'react';
import { Calendar, Activity, Clock, CheckSquare, Sparkles } from 'lucide-react';
import { calculateSleepStats } from '../services/agent';

export default function Dashboard({ profile, onNavigate }) {
  const [stats, setStats] = useState({
    total_logs: 0,
    average_duration_mins: 0,
    average_efficiency: 0.0,
    average_latency_mins: 0,
    average_awakenings: 0.0,
    average_quality: 0.0,
    average_alertness: 0.0
  });

  const [diaryDoneToday, setDiaryDoneToday] = useState(false);
  const [relaxationDoneToday, setRelaxationDoneToday] = useState(false);

  useEffect(() => {
    // 1. Calculate statistics
    const sleepStats = calculateSleepStats();
    setStats(sleepStats);
    
    // 2. Check if diary exists for today
    const diaries = JSON.parse(localStorage.getItem('sleep_diaries') || '[]');
    const todayStr = new Date().toISOString().split('T')[0];
    const loggedToday = diaries.some(d => d.date === todayStr);
    setDiaryDoneToday(loggedToday);
    
    // 3. Check if relaxation practiced today
    const practicedToday = localStorage.getItem(`relaxation_done_${todayStr}`) === 'true';
    setRelaxationDoneToday(practicedToday);
  }, [profile]);

  const cbtWeek = parseInt(localStorage.getItem('cbt_week') || '-1');
  
  const getWeekName = (week) => {
    const names = {
      '-1': 'Initial Interview',
      '0': 'Baseline Logging',
      '1': 'Week 1: Changing Thoughts',
      '2': 'Week 2: Establishing Habits',
      '3': 'Week 3: Lifestyle & Environment',
      '4': 'Week 4: Relaxation Response',
      '5': 'Week 5: Thinking Away Stress',
      '6': 'Week 6: Developing Attitudes'
    };
    return names[week.toString()] || 'CBT Maintenance';
  };

  const getWeekInstructions = (week) => {
    const instructions = {
      '-1': 'Engage in the voice chat to complete your initial sleep profile. Let\'s outline your goals.',
      '0': 'Log your sleep diary every morning. We need 7 days of logs to calculate your average sleep duration and efficiency.',
      '1': 'Read about sleep physiology. Identify your Negative Sleep Thoughts (NSTs) and replace them with Positive Sleep Thoughts (PSTs). Remember: you don\'t need 8 hours!',
      '2': 'Enforce sleep restriction! Limit your time in bed to your average sleep duration. Rise at the exact same time every day. No naps, and get out of bed if awake for 20 minutes.',
      '3': 'Expose yourself to morning bright light. Exercise for 20-30 minutes in the late afternoon. Complete caffeine cutoff by 12:00 PM, and avoid alcohol/nicotine.',
      '4': 'Practice the Relaxation Response. Use the breathing exercise tool in the Relaxation Room for 15-20 minutes every afternoon or before bed.',
      '5': 'Challenge stressful daytime thinking. Reframe your daytime negative self-talk and write down worries in a journal long before bedtime.',
      '6': 'Adopt sleep-resilient attitudes. Maintain consistency, commit to your sleep restriction windows, and view challenges as opportunities.'
    };
    return instructions[week.toString()] || 'Maintain your consistent rise times, sleep-promoting habits, and relaxation techniques.';
  };

  const formatMinsToHours = (totalMins) => {
    if (!totalMins) return '0 hrs';
    const hrs = Math.floor(totalMins / 60);
    const mins = totalMins % 60;
    return mins > 0 ? `${hrs}h ${mins}m` : `${hrs}h`;
  };

  // Convert cbtWeek to progress percentage
  // -1 to 6 represent 8 states
  const getProgressPercentage = () => {
    return Math.max(0, Math.min(100, Math.round(((cbtWeek + 1) / 7) * 100)));
  };

  return (
    <div>
      {/* CBT-I Phase Indicator */}
      <div className="glass-panel glowing">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <span className="cbt-badge">{getWeekName(cbtWeek)}</span>
            <h2 className="section-title" style={{ marginTop: '0.75rem' }}>CBT-I Program Status</h2>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
            <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--gold)' }}>
              {cbtWeek === -1 ? 'Interview' : cbtWeek === 0 ? 'Baseline' : `Week ${cbtWeek}/6`}
            </span>
          </div>
        </div>
        
        <p style={{ marginTop: '0.75rem', fontSize: '0.9rem', lineHeight: '1.5', color: 'var(--text-secondary)' }}>
          {getWeekInstructions(cbtWeek)}
        </p>
        
        <div className="progress-container">
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Start</span>
          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${getProgressPercentage()}%` }}></div>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--gold)', fontWeight: 600 }}>{getProgressPercentage()}% Complete</span>
        </div>
      </div>

      {/* Sleep Statistics Summary */}
      <div className="dashboard-grid">
        <div className="glass-panel stat-card">
          <Clock size={20} style={{ color: 'var(--gold)' }} />
          <span className="stat-val">{stats.average_efficiency}%</span>
          <span className="stat-label">Sleep Efficiency</span>
          <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
            Target: &gt; 85% normal sleepers
          </p>
        </div>

        <div className="glass-panel stat-card">
          <Activity size={20} style={{ color: 'var(--purple)' }} />
          <span className="stat-val">{formatMinsToHours(stats.average_duration_mins)}</span>
          <span className="stat-label">Average Sleep Time</span>
          <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
            Total actual sleep duration
          </p>
        </div>

        <div className="glass-panel stat-card">
          <Clock size={20} style={{ color: 'var(--cyan)' }} />
          <span className="stat-val">{stats.average_latency_mins}m</span>
          <span className="stat-label">Average Latency</span>
          <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
            Time to fall asleep
          </p>
        </div>

        <div className="glass-panel stat-card">
          <Calendar size={20} style={{ color: 'var(--text-secondary)' }} />
          <span className="stat-val">{stats.total_logs} / 7</span>
          <span className="stat-label">Baseline logs</span>
          <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
            Logs logged in baseline
          </p>
        </div>
      </div>

      {/* Today's Sleep Checklist */}
      <div className="glass-panel">
        <h3 className="section-title" style={{ fontSize: '1.25rem' }}>Today's Checklist</h3>
        <p className="section-subtitle">Maintain consistency in your daily CBT-I exercises.</p>
        
        <div className="checklist-item" onClick={() => onNavigate('diary')}>
          <div className={`checklist-checkbox ${diaryDoneToday ? 'checked' : ''}`}>
            {diaryDoneToday && '✓'}
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: diaryDoneToday ? 'var(--cyan)' : 'var(--text-primary)' }}>
              Complete Sleep Diary
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Report your sleep parameters from last night.
            </div>
          </div>
        </div>

        <div className="checklist-item" onClick={() => onNavigate('relaxation')}>
          <div className={`checklist-checkbox ${relaxationDoneToday ? 'checked' : ''}`}>
            {relaxationDoneToday && '✓'}
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: relaxationDoneToday ? 'var(--cyan)' : 'var(--text-primary)' }}>
              Practice Relaxation Response
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Perform 15 minutes of guided box breathing.
            </div>
          </div>
        </div>
        
        <div className="checklist-item" onClick={() => onNavigate('chat')}>
          <div className="checklist-checkbox checked">
            ✓
          </div>
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--cyan)' }}>
              Daily Advisor Check-in
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Speak with your sleep advisor to receive tips and restructure thoughts.
            </div>
          </div>
        </div>
      </div>
      
      {/* Quick Tips */}
      <div className="glass-panel" style={{ display: 'flex', gap: '1rem', alignItems: 'center', background: 'rgba(255, 190, 11, 0.05)', borderColor: 'rgba(255, 190, 11, 0.15)' }}>
        <Sparkles size={24} style={{ color: 'var(--gold)', flexShrink: 0 }} />
        <div style={{ fontSize: '0.85rem', lineHeight: '1.4', color: '#ffea9f' }}>
          <strong>CBT-I Fact:</strong> The "8-hour sleep myth" causes excessive anxiety. Most adults sleep perfectly healthy on 6.5 to 7.5 hours, and research shows 7-hour sleepers actually live longer!
        </div>
      </div>
    </div>
  );
}
