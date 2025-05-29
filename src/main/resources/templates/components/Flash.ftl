<#-- Flash.ftl: full-width flash banner with JS clear -->
<style>
  /* Full-width container at top */
  .flash-container {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    pointer-events: none;
    z-index: 1000;
  }
  /* Full-width message banner */
  .flash-message {
    position: relative;
    width: 100%;
    padding: 1rem 2rem;
    border-radius: 0;
    font-size: 1rem;
    font-weight: 500;
    text-align: center;
    box-shadow: none;
    animation: slideDown 0.4s ease-out;
    pointer-events: auto;
  }
  /* Close button */
  .flash-close {
    position: absolute;
    top: 0.5rem;
    right: 1rem;
    background: transparent;
    border: none;
    font-size: 1.5rem;
    line-height: 1;
    cursor: pointer;
    transition: transform 0.2s, color 0.2s;
    color: rgba(255,255,255,0.8);
  }
  .flash-close:hover {
    color: white;
    transform: rotate(90deg);
  }
  @keyframes slideDown {
    0%   { opacity: 0; transform: translateY(-100%); }
    100% { opacity: 1; transform: translateY(0);    }
  }
  /* Type variants */
  .flash-message.success { background: #38a169; }
  .flash-message.error   { background: #e53e3e; }
  .flash-message.warning { background: #dd6b20; }
  .flash-message.info    { background: #3182ce; }
</style>

<#-- Render full-width flash banner -->
<#if flash.has()>
  <div class="flash-container" id="flash-container">
    <div class="flash-message ${flash.type()}" id="flash-message">
      <button class="flash-close" aria-label="Close" onclick="
        document.getElementById('flash-container').style.display='none';
        document.cookie = 'flash_message=; max-age=0; path=/';
        document.cookie = 'flash_type=; max-age=0; path=/';
      ">&times;</button>
      ${flash.message()}
    </div>
  </div>
</#if>
